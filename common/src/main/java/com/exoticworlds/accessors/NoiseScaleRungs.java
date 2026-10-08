package com.exoticworlds.accessors;

import java.util.Map;

public interface NoiseScaleRungs {
    Map<Double, Double> toroidal$scaleRungs();

    void toroidal$scaleRungs(Map<Double, Double> rungs);
}
