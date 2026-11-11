package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import com.exoticworlds.accessors.PeriodicityMark;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.mojang.logging.LogUtils;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
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
        AtomicReference<SeamVerdict> slot = ((PeriodicityMark) level).exotic_worlds$seamVerdict();
        SeamVerdict known = slot.get();
        if (known != null) {
            return known;
        }

        synchronized (slot) {
            known = slot.get();
            if (known == null) {
                known = check(level);
                slot.set(known);
            }

            return known;
        }
    }

    public static List<SeamVerdict> verdictsOf(MinecraftServer server) {
        List<SeamVerdict> verdicts = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (WorldLoopAttachments.foldOf(level).isWrapped()) {
                verdicts.add(verdictOf(level));
            }
        }

        return verdicts;
    }

    private static SeamVerdict check(ServerLevel level) {
        String levelName = level.dimension().location().toString();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!ShapedChunkGenerator.tilesAtSeam(generator)) {
            LOGGER.info("[ew/world-loop] periodicity_skipped level={} generator={}",
                    levelName, generator.getClass().getSimpleName());
            return new SeamVerdict(level.dimension(), List.of(), SeamCauseSearch.generatorCause(generator));
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
        SeamVerdict verdict = new SeamVerdict(level.dimension(), breaks, null);
        report(levelName, probe, verdict, brokenSamples);
        return verdict;
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

    private static boolean sampleBroken(Probe probe, int x, int z, Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        SeamCauseSearch.Point surface = new SeamCauseSearch.Point(x, SAMPLE_Y, z);
        SeamCauseSearch.Point surfaceLapAway = probe.lap().away(surface);
        NoiseColumn columnHere = probe.generator().getBaseColumn(x, z, probe.level(), probe.randomState());
        NoiseColumn columnLapAway = probe.generator()
                .getBaseColumn(surfaceLapAway.x(), surfaceLapAway.z(), probe.level(), probe.randomState());
        boolean sampleBroken = collectColumn(broken, probe.level(), surface, columnHere, columnLapAway);

        SeamCauseSearch.Point quart = quartOf(surface);
        SeamCauseSearch.Point quartLapAway = quartOf(surfaceLapAway);
        Climate.TargetPoint climateHere = GenerationFoldContext.withFold(probe.fold(),
                () -> ClimateTargets.at(probe.sampler(), quart.x(), quart.y(), quart.z()));
        Climate.TargetPoint climateLapAway = GenerationFoldContext.withFold(probe.fold(),
                () -> ClimateTargets.at(probe.sampler(), quartLapAway.x(), quartLapAway.y(), quartLapAway.z()));
        SeamCauseSearch.Point climatePoint = new SeamCauseSearch.Point(QuartPos.toBlock(quart.x()),
                QuartPos.toBlock(quart.y()), QuartPos.toBlock(quart.z()));
        for (SeamField field : SeamField.values()) {
            if (field.climate() && field.climateOf(climateHere) != field.climateOf(climateLapAway)) {
                broken.computeIfAbsent(field, ignored -> new ArrayList<>()).add(climatePoint);
                sampleBroken = true;
            }
        }

        Holder<Biome> biomeHere = GenerationFoldContext.withFold(probe.fold(),
                () -> probe.generator().getBiomeSource().getNoiseBiome(quart.x(), quart.y(), quart.z(), probe.sampler()));
        Holder<Biome> biomeLapAway = GenerationFoldContext.withFold(probe.fold(),
                () -> probe.generator().getBiomeSource()
                        .getNoiseBiome(quartLapAway.x(), quartLapAway.y(), quartLapAway.z(), probe.sampler()));
        if (!biomeHere.equals(biomeLapAway)) {
            broken.computeIfAbsent(SeamField.BIOME, ignored -> new ArrayList<>()).add(climatePoint);
            sampleBroken = true;
        }

        return sampleBroken;
    }

    private static SeamCauseSearch.Point quartOf(SeamCauseSearch.Point block) {
        return new SeamCauseSearch.Point(QuartPos.fromBlock(block.x()), QuartPos.fromBlock(block.y()),
                QuartPos.fromBlock(block.z()));
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

    private static boolean collectColumn(Map<SeamField, List<SeamCauseSearch.Point>> broken,
            LevelHeightAccessor heightAccessor, SeamCauseSearch.Point surface, NoiseColumn here, NoiseColumn lapAway) {
        for (int y = heightAccessor.getMinBuildHeight(); y < heightAccessor.getMaxBuildHeight(); y++) {
            if (here.getBlock(y) != lapAway.getBlock(y)) {
                broken.computeIfAbsent(SeamField.COLUMN, ignored -> new ArrayList<>())
                        .add(new SeamCauseSearch.Point(surface.x(), y, surface.z()));
                return true;
            }
        }

        return false;
    }

    private PeriodicityCheck() {
    }
}
