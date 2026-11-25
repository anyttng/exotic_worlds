package com.exoticworlds.core.feature;

import java.util.Arrays;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public interface NoiseFeature extends GenerationFeature {
    double UNCHANGED = 1.0;

    default double[] layerFactors(WorldFold fold, Holder<NormalNoise> noise, NoiseStack stack, double baseScale,
            double verticalShare) {
        double[] factors = new double[stack.layers.length];
        Arrays.fill(factors, UNCHANGED);
        return factors;
    }

    default double warpFactor(WorldFold fold, Holder<NormalNoise> noise, double xzScale, double verticalShare) {
        return UNCHANGED;
    }

    default boolean isOffsetting(Holder<NormalNoise> noise) {
        return false;
    }
}
