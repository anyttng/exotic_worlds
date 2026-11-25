package com.exoticworlds.engine.noise;

import java.util.Arrays;
import java.util.List;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.feature.NoiseOffsets;

public final class NoiseFeatures {

    public static double[] layerFactors(List<NoiseFeature> features, NoiseMoment moment) {
        double[] product = new double[moment.stack().layers.length];
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

    private NoiseFeatures() {
    }
}
