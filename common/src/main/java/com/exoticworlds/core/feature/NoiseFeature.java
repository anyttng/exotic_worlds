package com.exoticworlds.core.feature;

import com.exoticworlds.core.fold.WorldFold;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public interface NoiseFeature extends GenerationFeature {
    double UNCHANGED = 1.0;

    default double scaleFactor(WorldFold fold, boolean isClimateField, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        return UNCHANGED;
    }

    default double warpFactor(WorldFold fold, DensityFunction.NoiseHolder noise, double xzScale,
            double verticalShare) {
        return UNCHANGED;
    }

    default boolean isOffsetting(Holder<NormalNoise.NoiseParameters> noise) {
        return false;
    }
}
