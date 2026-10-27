package com.exoticworlds.engine.noise;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;

import com.exoticworlds.accessors.PeriodicityMark;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.mojang.logging.LogUtils;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
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
            Climate.Sampler sampler, WorldFold fold, Direction.Axis lapAxis, int widthBlocks) {
    }

    public static void runOnce(ServerLevel level, WorldFold fold) {
        if (!((PeriodicityMark) level).exotic_worlds$claimPeriodicityCheck()) {
            return;
        }

        String levelName = level.dimension().identifier().toString();

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!ShapedChunkGenerator.tilesAtSeam(generator)) {
            LOGGER.info("[ew/world-loop] periodicity_skipped level={} generator={}",
                    levelName, generator.getClass().getSimpleName());
            return;
        }

        Probe probe = probeOf(level, generator, fold);
        Set<String> brokenFields = new LinkedHashSet<>();
        int brokenSamples = 0;
        for (int z : SAMPLE_Z) {
            for (int x : SAMPLE_X) {
                if (sampleBroken(probe, x, z, brokenFields)) {
                    brokenSamples++;
                }
            }
        }

        report(levelName, probe, brokenFields, brokenSamples);
    }

    private static Probe probeOf(ServerLevel level, ChunkGenerator generator, WorldFold fold) {
        RandomState randomState = level.getChunkSource().randomState();
        Climate.Sampler sampler = randomState.sampler();
        Direction.Axis lapAxis = fold.bounds().loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
        TranslationLattice lattice = fold.blockLattice();
        int widthBlocks = (lapAxis == Direction.Axis.X ? lattice.x() : lattice.z()).domainLength;
        return new Probe(level, generator, randomState, sampler, fold, lapAxis, widthBlocks);
    }

    private static boolean sampleBroken(Probe probe, int x, int z, Set<String> brokenFields) {
        int xLapAway = probe.lapAxis() == Direction.Axis.X ? x + probe.widthBlocks() : x;
        int zLapAway = probe.lapAxis() == Direction.Axis.Z ? z + probe.widthBlocks() : z;
        NoiseColumn columnHere = probe.generator().getBaseColumn(x, z, probe.level(), probe.randomState());
        NoiseColumn columnLapAway = probe.generator()
                .getBaseColumn(xLapAway, zLapAway, probe.level(), probe.randomState());
        boolean broken = collectColumn(brokenFields, probe.level(), columnHere, columnLapAway);

        int quartX = QuartPos.fromBlock(x);
        int quartZ = QuartPos.fromBlock(z);
        int quartY = QuartPos.fromBlock(SAMPLE_Y);
        int lapAwayQuartX = QuartPos.fromBlock(xLapAway);
        int lapAwayQuartZ = QuartPos.fromBlock(zLapAway);

        Climate.TargetPoint climateHere = GenerationFoldContext.withFold(probe.fold(),
                () -> ClimateTargets.at(probe.sampler(), quartX, quartY, quartZ));
        Climate.TargetPoint climateLapAway = GenerationFoldContext.withFold(probe.fold(),
                () -> ClimateTargets.at(probe.sampler(), lapAwayQuartX, quartY, lapAwayQuartZ));
        broken |= collectClimate(brokenFields, climateHere, climateLapAway);

        Holder<Biome> biomeHere = GenerationFoldContext.withFold(probe.fold(),
                () -> probe.generator().getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, probe.sampler()));
        Holder<Biome> biomeLapAway = GenerationFoldContext.withFold(probe.fold(),
                () -> probe.generator().getBiomeSource()
                        .getNoiseBiome(lapAwayQuartX, quartY, lapAwayQuartZ, probe.sampler()));
        if (!biomeHere.equals(biomeLapAway)) {
            brokenFields.add("biome");
            broken = true;
        }

        return broken;
    }

    private static void report(String levelName, Probe probe, Set<String> brokenFields, int brokenSamples) {
        String axisName = probe.lapAxis().getName().toUpperCase(Locale.ROOT);
        int widthBlocks = probe.widthBlocks();
        int samples = SAMPLE_X.length * SAMPLE_Z.length;
        if (brokenFields.isEmpty()) {
            LOGGER.info("[ew/world-loop] periodicity_ok level={} axis={} width_blocks={} samples={}",
                    levelName, axisName, widthBlocks, samples);
            return;
        }

        LOGGER.warn(
                "[ew/world-loop] periodicity_broken level={} axis={} width_blocks={} fields={} broken_samples={} samples={}",
                levelName, axisName, widthBlocks, String.join(",", brokenFields), brokenSamples, samples);
    }

    private static boolean collectColumn(Set<String> brokenFields, LevelHeightAccessor heightAccessor,
            NoiseColumn here, NoiseColumn lapAway) {
        for (int y = heightAccessor.getMinY(); y <= heightAccessor.getMaxY(); y++) {
            if (here.getBlock(y) != lapAway.getBlock(y)) {
                brokenFields.add("column");
                return true;
            }
        }

        return false;
    }

    private static boolean collectClimate(Set<String> brokenFields, Climate.TargetPoint here,
            Climate.TargetPoint lapAway) {
        boolean broken = collect(brokenFields, "temperature", here.temperature(), lapAway.temperature());
        broken |= collect(brokenFields, "humidity", here.humidity(), lapAway.humidity());
        broken |= collect(brokenFields, "continentalness", here.continentalness(), lapAway.continentalness());
        broken |= collect(brokenFields, "erosion", here.erosion(), lapAway.erosion());
        broken |= collect(brokenFields, "depth", here.depth(), lapAway.depth());
        broken |= collect(brokenFields, "weirdness", here.weirdness(), lapAway.weirdness());
        return broken;
    }

    private static boolean collect(Set<String> brokenFields, String field, long here, long lapAway) {
        if (here == lapAway) {
            return false;
        }

        brokenFields.add(field);
        return true;
    }

    private PeriodicityCheck() {
    }
}
