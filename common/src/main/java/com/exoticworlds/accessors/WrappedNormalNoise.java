package com.exoticworlds.accessors;

import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

public interface WrappedNormalNoise {
    double exotic_worlds$sampleWrapped(Context context, double x, double y, double z);
}
