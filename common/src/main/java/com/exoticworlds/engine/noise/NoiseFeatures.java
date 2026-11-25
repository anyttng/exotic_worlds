package com.exoticworlds.engine.noise;

import java.util.List;

import com.exoticworlds.accessors.NoiseFeatureHolder;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class NoiseFeatures {

    public static double scaleFactor(List<NoiseFeature> features, WorldFold fold, boolean isClimateField,
            DoubleList amplitudes, double lowestFreqInputFactor, double baseScale, double verticalShare) {
        double product = NoiseFeature.UNCHANGED;
        for (NoiseFeature feature : features) {
            product *= feature.scaleFactor(fold, isClimateField, amplitudes, lowestFreqInputFactor, baseScale,
                    verticalShare);
        }

        return product;
    }

    public static double warpDivisor(List<NoiseFeature> features, WorldFold fold, DensityFunction.NoiseHolder noise,
            double xzScale, double verticalShare) {
        double divisor = xzScale;
        for (NoiseFeature feature : features) {
            divisor *= feature.warpFactor(fold, noise, xzScale, verticalShare);
        }

        return divisor;
    }

    public static NoiseOffset offsetOf(List<NoiseFeature> features, NoiseOffsets offsets,
            Holder<NormalNoise.NoiseParameters> noise) {
        return offsets.sumOf(features.stream()
                .filter(feature -> feature.isOffsetting(noise))
                .map(GenerationFeature::getKey)
                .toList());
    }

    public static List<NoiseFeature> getFeatures(DensityFunction.NoiseHolder noise) {
        return noise.noise() instanceof NoiseFeatureHolder holder ? holder.exotic_worlds$getNoiseFeatures() : List.of();
    }

    private NoiseFeatures() {
    }
}
