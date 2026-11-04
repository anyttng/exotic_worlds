package com.exoticworlds.engine.gen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.net.DimensionChoicesPayload;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;

public final class DimensionChoices {
    private static final String REGION_FOLDER = "region";
    private static final String REGION_SUFFIX = ".mca";
    private static final List<ResourceKey<LevelStem>> VANILLA_ORDER =
            List.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END);
    private static final Comparator<ResourceKey<LevelStem>> STEM_ORDER = Comparator
            .comparingInt(DimensionChoices::vanillaIndex)
            .thenComparing(key -> key.identifier().toString());

    private static final Object TABLE_LOCK = new Object();
    private static final Map<ResourceKey<LevelStem>, Optional<DimensionTable.Entry>> PENDING =
            new ConcurrentHashMap<>();

    public enum Refusal {
        VANILLA,
        UNKNOWN,
        DECIDED,
        GENERATED
    }

    public static List<DimensionChoicesPayload.Row> rows(MinecraftServer server) {
        DimensionTable table = storedTable(server);
        return stems(server).map(key -> {
            Refusal refusal = refusalOf(server, key);
            DimensionTable.Entry entry = table.entry(key);
            return new DimensionChoicesPayload.Row(key, Optional.ofNullable(refusal),
                    entry != null ? entry : refusal == null ? DimensionTable.Entry.FROM_OVERWORLD : liveEntry(server, key));
        }).toList();
    }

    public static List<ResourceKey<LevelStem>> undecided(MinecraftServer server) {
        if (!isShaped(server)) {
            return List.of();
        }

        return stems(server).filter(key -> refusalOf(server, key) == null).toList();
    }

    public static @Nullable Refusal choose(MinecraftServer server, ResourceKey<LevelStem> key,
            DimensionTable.Entry entry) {
        Refusal refusal = refusalOf(server, key);
        if (refusal != null) {
            return refusal;
        }

        synchronized (TABLE_LOCK) {
            PENDING.putIfAbsent(key, Optional.ofNullable(storedTable(server).entry(key)));
            writeTable(server, table -> table.with(key, entry));
        }

        return null;
    }

    public static void recordFirstStart(MinecraftServer server) {
        PENDING.clear();
        if (server.getWorldData().overworldData().isInitialized() || !isShaped(server)) {
            return;
        }

        DimensionTable table = storedTable(server);
        List<ResourceKey<LevelStem>> present = stems(server)
                .filter(key -> !DimensionTable.isVanillaStem(key) && table.entry(key) == null)
                .toList();
        if (present.isEmpty()) {
            return;
        }

        writeTable(server, current -> {
            DimensionTable recorded = current;
            for (ResourceKey<LevelStem> key : present) {
                recorded = recorded.with(key, DimensionTable.Entry.FROM_OVERWORLD);
            }

            return recorded;
        });
    }

    public static void dropPending(ServerLevel level) {
        if (PENDING.isEmpty()) {
            return;
        }

        ResourceKey<LevelStem> key = Registries.levelToLevelStem(level.dimension());
        if (!PENDING.containsKey(key)) {
            return;
        }

        MinecraftServer server = level.getServer();
        synchronized (TABLE_LOCK) {
            Optional<DimensionTable.Entry> previous = PENDING.remove(key);
            if (previous == null) {
                return;
            }

            writeTable(server, table -> previous.map(entry -> table.with(key, entry)).orElseGet(() -> table.without(key)));
        }

        server.execute(() -> DimensionChoicesCommand.tellDropped(server, key));
    }

    private static @Nullable Refusal refusalOf(MinecraftServer server, ResourceKey<LevelStem> key) {
        if (DimensionTable.isVanillaStem(key)) {
            return Refusal.VANILLA;
        }

        ServerLevel level = server.getLevel(Registries.levelStemToLevel(key));
        if (level == null) {
            return Refusal.UNKNOWN;
        }

        if (storedTable(server).entry(key) != null) {
            return Refusal.DECIDED;
        }

        return level.getChunkSource().getLoadedChunksCount() > 0 || hasRegionFiles(server, level.dimension())
                ? Refusal.GENERATED
                : null;
    }

    private static boolean hasRegionFiles(MinecraftServer server, ResourceKey<Level> dimension) {
        Path region = DimensionType.getStorageFolder(dimension, server.getWorldPath(LevelResource.ROOT))
                .resolve(REGION_FOLDER);
        if (!Files.isDirectory(region)) {
            return false;
        }

        try (Stream<Path> files = Files.list(region)) {
            return files.anyMatch(file -> file.getFileName().toString().endsWith(REGION_SUFFIX));
        } catch (IOException unreadable) {
            return true;
        }
    }

    private static void writeTable(MinecraftServer server, UnaryOperator<DimensionTable> change) {
        synchronized (TABLE_LOCK) {
            for (ChunkGenerator generator : overworldGenerators(server)) {
                if (generator instanceof ShapeStamp stamp) {
                    stamp.exotic_worlds$stampDimensionTable(change.apply(stamp.exotic_worlds$dimensionTable()));
                }
            }

            server.getWorldGenSettings().setDirty();
        }
    }

    private static List<ChunkGenerator> overworldGenerators(MinecraftServer server) {
        List<ChunkGenerator> generators = new ArrayList<>(2);
        server.getWorldGenSettings().dimensions().get(LevelStem.OVERWORLD)
                .ifPresent(stem -> generators.add(stem.generator()));
        LevelStem live = liveStems(server).getValue(LevelStem.OVERWORLD);
        if (live != null && generators.stream().noneMatch(generator -> generator == live.generator())) {
            generators.add(live.generator());
        }

        return generators;
    }

    private static DimensionTable storedTable(MinecraftServer server) {
        return ShapedDimensions.dimensionTableOf(server.getWorldGenSettings().dimensions());
    }

    private static boolean isShaped(MinecraftServer server) {
        return !storedTable(server).isEmpty() || liveStems(server).stream()
                .anyMatch(stem -> ShapedChunkGenerator.carriedShapeOf(stem.generator()) != null);
    }

    private static DimensionTable.Entry liveEntry(MinecraftServer server, ResourceKey<LevelStem> key) {
        LevelStem stem = liveStems(server).getValue(key);
        CarriedShape carried = stem == null ? null : ShapedChunkGenerator.carriedShapeOf(stem.generator());
        return carried == null ? DimensionTable.Entry.UNWRAPPED : new DimensionTable.Entry(carried);
    }

    private static Stream<ResourceKey<LevelStem>> stems(MinecraftServer server) {
        return liveStems(server).registryKeySet().stream().sorted(STEM_ORDER);
    }

    private static Registry<LevelStem> liveStems(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
    }

    private static int vanillaIndex(ResourceKey<LevelStem> key) {
        int index = VANILLA_ORDER.indexOf(key);
        return index < 0 ? VANILLA_ORDER.size() : index;
    }

    private DimensionChoices() {
    }
}
