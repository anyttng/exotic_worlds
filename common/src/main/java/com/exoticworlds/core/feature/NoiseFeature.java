package com.exoticworlds.core.feature;

import java.util.Arrays;

public interface NoiseFeature extends GenerationFeature {
    double UNCHANGED = 1.0;

    default double[] layerFactors(NoiseMoment moment) {
        double[] factors = new double[moment.layerFrequencies().size()];
        Arrays.fill(factors, UNCHANGED);
        return factors;
    }

    default double warpFactor(NoiseMoment moment) {
        return UNCHANGED;
    }

    default boolean isOffsetting(NoiseMoment moment) {
        return false;
    }

    default double climateFieldFactor(ClimateFieldMoment moment) {
        return UNCHANGED;
    }
}
