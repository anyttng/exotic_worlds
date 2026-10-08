package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class FoldedPlacementCounts {
    public static int noiseBased(WorldFold fold, int x, int z, double noiseFactor, double noiseOffset,
            int noiseToCountRatio) {
        return (int) Math.ceil((biomeInfo(fold, noiseFactor, x, z) + noiseOffset) * noiseToCountRatio);
    }

    public static int noiseThreshold(WorldFold fold, int x, int z, double noiseLevel, int belowNoise, int aboveNoise) {
        return biomeInfo(fold, NoiseConstants.BIOME_INFO_THRESHOLD_DIVISOR, x, z) < noiseLevel
                ? belowNoise
                : aboveNoise;
    }

    @SuppressWarnings("removal")
    private static double biomeInfo(WorldFold fold, double divisor, int x, int z) {
        return PeriodicSimplexSampler.sample((SimplexNoise) Biome.BIOME_INFO_NOISE, fold, 1.0 / divisor, x, z);
    }

    private FoldedPlacementCounts() {
    }
}
