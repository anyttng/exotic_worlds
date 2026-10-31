package com.exoticworlds.core;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class DimensionTable {
    public static final String KEY = "dimensions";

    public static final DimensionTable EMPTY = new DimensionTable(Map.of());

    private static final String NO_KEY_PREFIX = "";

    private static final MapCodec<Entry> ENTRY_CODEC = RecordCodecBuilder.<Stated>mapCodec(instance -> instance.group(
            CarriedShape.SHAPE_CODEC.optionalFieldOf(CarriedShape.WRAPPING_KEY).forGetter(Stated::shape),
            GenerationOptions.mapCodec(NO_KEY_PREFIX).forGetter(Stated::options)
    ).apply(instance, Stated::new)).xmap(Stated::entry, Stated::of);

    public static final Codec<DimensionTable> CODEC = Codec
            .unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), ENTRY_CODEC.codec())
            .validate(DimensionTable::refuseVanillaStems)
            .xmap(DimensionTable::new, table -> table.entries);

    private final Map<ResourceKey<LevelStem>, Entry> entries;

    private DimensionTable(Map<ResourceKey<LevelStem>, Entry> entries) {
        this.entries = Map.copyOf(entries);
    }

    public static DimensionTable of(Map<ResourceKey<LevelStem>, Entry> entries) {
        return new DimensionTable(refuseVanillaStems(entries).getOrThrow(IllegalArgumentException::new));
    }

    public @Nullable Entry entry(ResourceKey<LevelStem> key) {
        return this.entries.get(key);
    }

    public Map<ResourceKey<LevelStem>, Entry> entries() {
        return this.entries;
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public static boolean isVanillaStem(ResourceKey<LevelStem> key) {
        return VanillaStems.KEYS.contains(key);
    }

    private static DataResult<Map<ResourceKey<LevelStem>, Entry>> refuseVanillaStems(
            Map<ResourceKey<LevelStem>, Entry> entries) {
        for (ResourceKey<LevelStem> key : entries.keySet()) {
            if (isVanillaStem(key)) {
                return DataResult.error(() -> "A dimension table names " + key.identifier()
                        + ", which carries its shape on its own generator");
            }
        }

        return DataResult.success(entries);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DimensionTable table && this.entries.equals(table.entries);
    }

    @Override
    public int hashCode() {
        return this.entries.hashCode();
    }

    private static final class VanillaStems {
        // LevelStem's initializer reads ChunkGenerator.CODEC, which our codec mixin builds from this class.
        private static final Set<ResourceKey<LevelStem>> KEYS = Set.of(LevelStem.OVERWORLD, LevelStem.NETHER,
                LevelStem.END);
    }

    public record Entry(@Nullable CarriedShape carried) {
        public static final Entry UNWRAPPED = new Entry(null);
    }

    private record Stated(Optional<FlatShape> shape, GenerationOptions options) {
        private static Stated of(Entry entry) {
            CarriedShape carried = entry.carried();
            return carried == null
                    ? new Stated(Optional.empty(), GenerationOptions.DEFAULT)
                    : new Stated(Optional.of(carried.shape()), carried.generationOptions());
        }

        private Entry entry() {
            return this.shape.map(flat -> new Entry(new CarriedShape(flat, this.options))).orElse(Entry.UNWRAPPED);
        }
    }
}
