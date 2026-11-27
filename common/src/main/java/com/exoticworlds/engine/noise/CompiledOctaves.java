package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

public interface CompiledOctaves {
    float sample(double x, double y, double z);

    boolean isIndexable();

    static double layerScale(double scale, double @Nullable [] layerFactors, int layer, double frequency) {
        return layerFactors == null ? scale * frequency : scale * layerFactors[layer] * frequency;
    }
}
