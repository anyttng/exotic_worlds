package com.exoticworlds.accessors;

import com.exoticworlds.engine.noise.GenerationFoldContext.Context;

public interface WrappedImprovedNoise {
    double exotic_worlds$sampleWrapped(Context context, double x, double y, double z, double yScale, double yFudge);
}
