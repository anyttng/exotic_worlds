package com.exoticworlds.engine.noise;

import java.util.Arrays;
import java.util.List;

import com.exoticworlds.accessors.NoiseOriginHolder;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class NoiseFeatures {

    public static double[] layerFactors(List<NoiseFeature> features, NoiseMoment moment) {
        double[] product = new double[moment.layerFrequencies().size()];
        Arrays.fill(product, NoiseFeature.UNCHANGED);
        for (NoiseFeature feature : features) {
            double[] factors = feature.layerFactors(moment);
            for (int layer = 0; layer < product.length; layer++) {
                product[layer] *= factors[layer];
            }
        }

        return product;
    }

    public static double warpDivisor(List<NoiseFeature> features, NoiseMoment moment) {
        double divisor = moment.xzScale();
        for (NoiseFeature feature : features) {
            divisor *= feature.warpFactor(moment);
        }

        return divisor;
    }

    public static NoiseOffset offsetOf(List<NoiseFeature> features, NoiseOffsets offsets, NoiseMoment moment) {
        return offsets.sumOf(features.stream()
                .filter(feature -> feature.isOffsetting(moment))
                .map(GenerationFeature::getKey)
                .toList());
    }

    public static NoiseOrigin getOrigin(DensityFunction.NoiseHolder noise) {
        return noise.noise() instanceof NoiseOriginHolder holder ? holder.exotic_worlds$getNoiseOrigin()
                : NoiseOrigin.NONE;
    }

    private NoiseFeatures() {
    }
}
