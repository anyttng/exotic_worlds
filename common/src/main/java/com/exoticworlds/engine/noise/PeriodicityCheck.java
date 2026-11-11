package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.exoticworlds.accessors.BiomeZoomOwner;
import com.exoticworlds.accessors.PeriodicityMark;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public final class PeriodicityCheck {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int[] SAMPLE_X = {0, 100, 255};
    private static final int[] SAMPLE_Z = {0, 137, -211};

    private static final int SAMPLE_Y = 64;

    private record Probe(ServerLevel level, ChunkGenerator generator, RandomState randomState,
            Climate.Sampler sampler, WorldFold fold, SeamCauseSearch.Lap lap) {
    }

    public static SeamVerdict verdictOf(ServerLevel level) {
        return cached(((PeriodicityMark) level).exotic_worlds$seamVerdict(), () -> check(level));
    }

    public static SeamVerdict fullVerdictOf(ServerLevel level) {
        return cached(((PeriodicityMark) level).exotic_worlds$fullSeamVerdict(), () -> fullCheck(level));
    }

    public static List<SeamVerdict> verdictsOf(MinecraftServer server) {
        return wrappedLevels(server).stream().map(PeriodicityCheck::verdictOf).toList();
    }

    public static List<SeamVerdict> fullVerdictsOf(MinecraftServer server) {
        return wrappedLevels(server).stream().map(PeriodicityCheck::fullVerdictOf).toList();
    }

    private static List<ServerLevel> wrappedLevels(MinecraftServer server) {
        List<ServerLevel> levels = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (WorldLoopAttachments.foldOf(level).isWrapped()) {
                levels.add(level);
            }
        }

        return levels;
    }

    private static SeamVerdict cached(AtomicReference<SeamVerdict> slot, Supplier<SeamVerdict> compute) {
        SeamVerdict known = slot.get();
        if (known != null) {
            return known;
        }

        synchronized (slot) {
            known = slot.get();
            if (known == null) {
                known = compute.get();
                slot.set(known);
            }

            return known;
        }
    }

    private static SeamVerdict check(ServerLevel level) {
        String levelName = level.dimension().identifier().toString();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!ShapedChunkGenerator.tilesAtSeam(generator)) {
            LOGGER.info("[ew/world-loop] periodicity_skipped level={} generator={}",
                    levelName, generator.getClass().getSimpleName());
            return new SeamVerdict(level.dimension(), List.of(), SeamCauseSearch.generatorCause(generator), 0);
        }

        Probe probe = probeOf(level, generator, WorldLoopAttachments.foldOf(level));
        Map<SeamField, List<SeamCauseSearch.Point>> broken = new LinkedHashMap<>();
        int brokenSamples = 0;
        for (int z : SAMPLE_Z) {
            for (int x : SAMPLE_X) {
                if (sampleBroken(probe, x, z, broken)) {
                    brokenSamples++;
                }
            }
        }

        List<SeamVerdict.Break> breaks = broken.isEmpty()
                ? List.of()
                : SeamCauseSearch.causesOf(broken, probe.lap(), probe.randomState(), probe.fold(),
                        generator.getBiomeSource());
        SeamVerdict verdict = new SeamVerdict(level.dimension(), breaks, null, 0);
        report(levelName, probe, verdict, brokenSamples);
        return verdict;
    }

    private static SeamVerdict fullCheck(ServerLevel level) {
        SeamVerdict base = verdictOf(level);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (base.unchecked() != null || !(generator instanceof NoiseBasedChunkGenerator noise)) {
            return base;
        }

        Map<String, SeamVerdict.Break> twinBreaks = new LinkedHashMap<>();
        int twinsRead = readTwins(probeOf(level, generator, WorldLoopAttachments.foldOf(level)), noise, twinBreaks);
        List<SeamVerdict.Break> breaks = new ArrayList<>(base.breaks());
        breaks.addAll(twinBreaks.values());
        return new SeamVerdict(level.dimension(), breaks, null, twinsRead);
    }

    private static Probe probeOf(ServerLevel level, ChunkGenerator generator, WorldFold fold) {
        RandomState randomState = level.getChunkSource().randomState();
        Climate.Sampler sampler = randomState.sampler();
        Direction.Axis lapAxis = fold.bounds().loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
        TranslationLattice lattice = fold.blockLattice();
        int widthBlocks = (lapAxis == Direction.Axis.X ? lattice.x() : lattice.z()).domainLength;
        return new Probe(level, generator, randomState, sampler, fold,
                new SeamCauseSearch.Lap(lapAxis, widthBlocks));
    }

    private static int readTwins(Probe probe, NoiseBasedChunkGenerator generator,
            Map<String, SeamVerdict.Break> first) {
        BiomeManager biomeManager = new BiomeManager(
                (quartX, quartY, quartZ) -> generator.getBiomeSource()
                        .getNoiseBiome(quartX, quartY, quartZ, probe.sampler()),
                BiomeManager.obfuscateSeed(probe.level().getSeed()));
        ((BiomeZoomOwner) biomeManager).exotic_worlds$setOwner(probe.level());
        LapTwinChunks.Inputs inputs = new LapTwinChunks.Inputs(probe.level(), generator, probe.randomState(),
                biomeManager, probe.fold(), probe.lap());
        int read = 0;
        for (int z : SAMPLE_Z) {
            for (int x : SAMPLE_X) {
                LapTwinChunks.Reading reading = LapTwinChunks.read(inputs, ChunkPos.containing(new BlockPos(x, 0, z)));
                for (SeamVerdict.Break found : reading.breaks()) {
                    first.putIfAbsent(found.field(), found);
                }

                read += reading.read() ? 1 : 0;
            }
        }

        return read;
    }

    private static boolean sampleBroken(Probe probe, int x, int z, Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        SeamCauseSearch.Point surface = new SeamCauseSearch.Point(x, SAMPLE_Y, z);
        boolean column = collectColumn(probe, surface, broken);
        boolean climate = collectClimate(probe, surface, broken);
        boolean biome = collectBiome(probe, surface, broken);
        return column || climate || biome;
    }

    private static boolean collectColumn(Probe probe, SeamCauseSearch.Point surface,
            Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        SeamCauseSearch.Point lapAway = probe.lap().away(surface);
        NoiseColumn here = probe.generator()
                .getBaseColumn(surface.x(), surface.z(), probe.level(), probe.randomState());
        NoiseColumn there = probe.generator()
                .getBaseColumn(lapAway.x(), lapAway.z(), probe.level(), probe.randomState());
        LevelHeightAccessor height = probe.level();
        for (int y = height.getMinY(); y <= height.getMaxY(); y++) {
            if (here.getBlock(y) != there.getBlock(y)) {
                broken.computeIfAbsent(SeamField.COLUMN, _ -> new ArrayList<>())
                        .add(new SeamCauseSearch.Point(surface.x(), y, surface.z()));
                return true;
            }
        }

        return false;
    }

    private static boolean collectClimate(Probe probe, SeamCauseSearch.Point surface,
            Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        SeamCauseSearch.Point quart = quartOf(surface);
        SeamCauseSearch.Point quartLapAway = quartOf(probe.lap().away(surface));
        Climate.TargetPoint here = GenerationFoldContext.withFold(probe.fold(),
                () -> ClimateTargets.at(probe.sampler(), quart.x(), quart.y(), quart.z()));
        Climate.TargetPoint there = GenerationFoldContext.withFold(probe.fold(),
                () -> ClimateTargets.at(probe.sampler(), quartLapAway.x(), quartLapAway.y(), quartLapAway.z()));
        boolean found = false;
        for (SeamField field : SeamField.values()) {
            if (field.climate() && field.climateOf(here) != field.climateOf(there)) {
                broken.computeIfAbsent(field, _ -> new ArrayList<>()).add(blockOf(quart));
                found = true;
            }
        }

        return found;
    }

    private static boolean collectBiome(Probe probe, SeamCauseSearch.Point surface,
            Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        SeamCauseSearch.Point quart = quartOf(surface);
        SeamCauseSearch.Point quartLapAway = quartOf(probe.lap().away(surface));
        Holder<Biome> here = GenerationFoldContext.withFold(probe.fold(),
                () -> probe.generator().getBiomeSource().getNoiseBiome(quart.x(), quart.y(), quart.z(), probe.sampler()));
        Holder<Biome> there = GenerationFoldContext.withFold(probe.fold(),
                () -> probe.generator().getBiomeSource()
                        .getNoiseBiome(quartLapAway.x(), quartLapAway.y(), quartLapAway.z(), probe.sampler()));
        if (here.equals(there)) {
            return false;
        }

        broken.computeIfAbsent(SeamField.BIOME, _ -> new ArrayList<>()).add(blockOf(quart));
        return true;
    }

    private static SeamCauseSearch.Point quartOf(SeamCauseSearch.Point block) {
        return new SeamCauseSearch.Point(QuartPos.fromBlock(block.x()), QuartPos.fromBlock(block.y()),
                QuartPos.fromBlock(block.z()));
    }

    private static SeamCauseSearch.Point blockOf(SeamCauseSearch.Point quart) {
        return new SeamCauseSearch.Point(QuartPos.toBlock(quart.x()), QuartPos.toBlock(quart.y()),
                QuartPos.toBlock(quart.z()));
    }

    private static void report(String levelName, Probe probe, SeamVerdict verdict, int brokenSamples) {
        String axisName = probe.lap().axis().getName().toUpperCase(Locale.ROOT);
        int widthBlocks = probe.lap().widthBlocks();
        int samples = SAMPLE_X.length * SAMPLE_Z.length;
        if (!verdict.broken()) {
            LOGGER.info("[ew/world-loop] periodicity_ok level={} axis={} width_blocks={} samples={}",
                    levelName, axisName, widthBlocks, samples);
            return;
        }

        LOGGER.warn(
                "[ew/world-loop] periodicity_broken level={} axis={} width_blocks={} fields={} causes={} broken_samples={} samples={}",
                levelName, axisName, widthBlocks, verdict.fields(), verdict.causes(), brokenSamples, samples);
    }

    private PeriodicityCheck() {
    }
}
