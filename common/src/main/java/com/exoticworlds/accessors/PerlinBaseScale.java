package com.exoticworlds.accessors;

import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

public interface PerlinBaseScale {
    double toroidal$baseScale(Context generation);
}
