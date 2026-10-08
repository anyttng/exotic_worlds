package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.engine.net.TagPositions.Nesting;
import com.exoticworlds.engine.net.TagPositions.PositionShape;
import com.exoticworlds.engine.net.TagPositions.TagPosition;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record PositionRows(Map<ResourceLocation, List<TagPosition>> blockEntities,
        Map<ResourceLocation, List<TagPosition>> entities, Set<ResourceLocation> components,
        Set<ResourceLocation> deny) {
    public static final String DIRECTORY = ExoticWorlds.MODID;
    public static final String FILE_NAME = "positions";

    public static final PositionRows EMPTY = new PositionRows(Map.of(), Map.of(), Set.of(), Set.of());

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BLOCK_ENTITIES_KEY = "block_entities";
    private static final String ENTITIES_KEY = "entities";
    private static final String COMPONENTS_KEY = "components";
    private static final String DENY_KEY = "deny";

    private record Container(Nesting nesting, Map<String, PositionShape> keys) {
    }

    private static final Codec<PositionShape> SHAPE_CODEC =
            Codec.STRING.comapFlatMap(PositionRows::shapeNamed, PositionRows::nameOf);

    private static final Codec<Map<String, PositionShape>> KEYS_CODEC = Codec.unboundedMap(Codec.STRING, SHAPE_CODEC);

    private static final Codec<Container> CONTAINER_CODEC = Codec.either(KEYS_CODEC, KEYS_CODEC.listOf(1, 1)).xmap(
            spelled -> spelled.map(
                    keys -> new Container(Nesting.COMPOUND, keys),
                    each -> new Container(Nesting.EACH_OF_LIST, each.getFirst())),
            PositionRows::spellingOf);

    private static final Codec<List<TagPosition>> SUBJECT_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.either(SHAPE_CODEC, CONTAINER_CODEC))
                    .xmap(PositionRows::positionsAt, PositionRows::addressesOf);

    public static final Codec<Map<ResourceLocation, List<TagPosition>>> SUBJECTS_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, SUBJECT_CODEC);

    static final Codec<Set<ResourceLocation>> IDS_CODEC =
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf);

    public static final Codec<PositionRows> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    SUBJECTS_CODEC.optionalFieldOf(BLOCK_ENTITIES_KEY, Map.of()).forGetter(PositionRows::blockEntities),
                    SUBJECTS_CODEC.optionalFieldOf(ENTITIES_KEY, Map.of()).forGetter(PositionRows::entities),
                    IDS_CODEC.optionalFieldOf(COMPONENTS_KEY, Set.of()).forGetter(PositionRows::components),
                    IDS_CODEC.optionalFieldOf(DENY_KEY, Set.of()).forGetter(PositionRows::deny))
            .apply(instance, PositionRows::new));

    public static PositionRows merge(Map<ResourceLocation, PositionRows> files, Predicate<ResourceLocation> blockEntityTypeKnown,
            Predicate<ResourceLocation> entityTypeKnown, Predicate<ResourceLocation> componentTypeKnown) {
        Map<ResourceLocation, List<TagPosition>> blockEntities = new LinkedHashMap<>();
        Map<ResourceLocation, List<TagPosition>> entities = new LinkedHashMap<>();
        Set<ResourceLocation> components = new HashSet<>();
        Set<ResourceLocation> deny = new HashSet<>();
        files.forEach((file, rows) -> {
            mergeInto(blockEntities, file, BLOCK_ENTITIES_KEY, rows.blockEntities(), blockEntityTypeKnown);
            mergeInto(entities, file, ENTITIES_KEY, rows.entities(), entityTypeKnown);
            rows.components().forEach(id -> {
                if (componentTypeKnown.test(id)) {
                    components.add(id);
                } else {
                    warnUnknown(file, COMPONENTS_KEY, id);
                }
            });
            deny.addAll(rows.deny());
        });

        return new PositionRows(Map.copyOf(blockEntities), Map.copyOf(entities), Set.copyOf(components),
                Set.copyOf(deny));
    }

    private static void mergeInto(Map<ResourceLocation, List<TagPosition>> merged, ResourceLocation file, String section,
            Map<ResourceLocation, List<TagPosition>> rows, Predicate<ResourceLocation> known) {
        rows.forEach((id, positions) -> {
            if (!known.test(id)) {
                warnUnknown(file, section, id);
                return;
            }

            List<TagPosition> joined = new ArrayList<>(merged.getOrDefault(id, List.of()));
            joined.addAll(positions);
            merged.put(id, List.copyOf(joined));
        });
    }

    private static void warnUnknown(ResourceLocation file, String section, ResourceLocation id) {
        LOGGER.warn("The {} file of namespace {} names {} under {}, which no mod registers; its rows are skipped",
                FILE_NAME, file.getNamespace(), id, section);
    }

    private static List<TagPosition> positionsAt(Map<String, Either<PositionShape, Container>> addresses) {
        List<TagPosition> positions = new ArrayList<>();
        addresses.forEach((name, address) -> address
                .ifLeft(shape -> positions.add(new TagPosition(name, shape)))
                .ifRight(container -> container.keys().forEach((key, shape) ->
                        positions.add(new TagPosition(container.nesting(), name, key, shape)))));
        return List.copyOf(positions);
    }

    private static Map<String, Either<PositionShape, Container>> addressesOf(List<TagPosition> positions) {
        Map<String, Either<PositionShape, Container>> addresses = new LinkedHashMap<>();
        Map<String, Map<String, PositionShape>> compounds = new LinkedHashMap<>();
        Map<String, Map<String, PositionShape>> lists = new LinkedHashMap<>();
        for (TagPosition position : positions) {
            if (position.nesting() == Nesting.TOP) {
                addresses.put(position.keys().getFirst(), Either.left(position.shape()));
            } else {
                (position.nesting() == Nesting.COMPOUND ? compounds : lists)
                        .computeIfAbsent(position.container(), container -> new LinkedHashMap<>())
                        .put(position.keys().getFirst(), position.shape());
            }
        }

        compounds.forEach((name, keys) -> addresses.put(name, Either.right(new Container(Nesting.COMPOUND, keys))));
        lists.forEach((name, keys) -> addresses.put(name, Either.right(new Container(Nesting.EACH_OF_LIST, keys))));
        return addresses;
    }

    private static Either<Map<String, PositionShape>, List<Map<String, PositionShape>>> spellingOf(Container container) {
        if (container.nesting() == Nesting.COMPOUND) {
            return Either.left(container.keys());
        }

        return Either.right(List.of(container.keys()));
    }

    private static DataResult<PositionShape> shapeNamed(String name) {
        for (PositionShape shape : PositionShape.values()) {
            if (shape.keyCount() == 1 && nameOf(shape).equals(name)) {
                return DataResult.success(shape);
            }
        }

        return DataResult.error(() -> "No position shape is named " + name);
    }

    private static String nameOf(PositionShape shape) {
        return shape.name().toLowerCase(Locale.ROOT);
    }
}
