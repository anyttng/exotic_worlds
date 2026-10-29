package com.exoticworlds.accessors;

import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

public interface WrappedPerlinNoise {
    double exotic_worlds$sampleWrapped(Context context, double x, double y, double z, double yScale, double yFudge);
}
