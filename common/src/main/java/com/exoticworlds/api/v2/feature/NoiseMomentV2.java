package com.exoticworlds.api.v2.feature;

import java.util.Arrays;

/**
 * The moment each terrain noise is compiled onto the loop. A feature stretches or shrinks a noise here, or marks it as
 * one it shifts. Every method leaves the noise as it is unless overridden, and where several features act on one
 * noise their factors multiply and their offsets add.
 */
public interface NoiseMomentV2 {
    /** The factor that changes nothing. */
    double UNCHANGED = 1.0;

    /**
     * How many times finer each layer of the noise becomes, one factor per layer, in the noise's own layer order. A
     * factor of two halves the size of what that layer draws.
     */
    default double[] layerFactors(NoiseV2 noise) {
        double[] factors = new double[noise.getLayerCount()];
        Arrays.fill(factors, UNCHANGED);
        return factors;
    }

    /** How many times the scale a domain warp of this noise is divided by grows. */
    default double warpFactor(NoiseV2 noise) {
        return UNCHANGED;
    }

    /** Whether the feature shifts this noise by the offset it sets through {@link RandomStateV2#setNoiseOffset}. */
    default boolean isOffsetting(NoiseV2 noise) {
        return false;
    }
}
