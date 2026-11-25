package com.exoticworlds.engine.noise;

import java.util.Arrays;
import java.util.List;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class NoiseFeatures {

    public static double[] layerFactors(List<NoiseFeature> features, WorldFold fold, Holder<NormalNoise> noise,
            NoiseStack stack, double baseScale, double verticalShare) {
        double[] product = new double[stack.layers.length];
        Arrays.fill(product, NoiseFeature.UNCHANGED);
        for (NoiseFeature feature : features) {
            double[] factors = feature.layerFactors(fold, noise, stack, baseScale, verticalShare);
            for (int layer = 0; layer < product.length; layer++) {
                product[layer] *= factors[layer];
            }
        }

        return product;
    }

    public static double warpDivisor(List<NoiseFeature> features, WorldFold fold, Holder<NormalNoise> noise,
            double xzScale, double verticalShare) {
        double divisor = xzScale;
        for (NoiseFeature feature : features) {
            divisor *= feature.warpFactor(fold, noise, xzScale, verticalShare);
        }

        return divisor;
    }

    public static NoiseOffset offsetOf(List<NoiseFeature> features, NoiseOffsets offsets, Holder<NormalNoise> noise) {
        return offsets.sumOf(features.stream()
                .filter(feature -> feature.isOffsetting(noise))
                .map(GenerationFeature::getKey)
                .toList());
    }

    private NoiseFeatures() {
    }
}
