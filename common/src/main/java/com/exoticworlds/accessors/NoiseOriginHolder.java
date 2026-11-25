package com.exoticworlds.accessors;

import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.NoiseOrigin;

public interface NoiseOriginHolder {
    NoiseOrigin exotic_worlds$getNoiseOrigin();

    void exotic_worlds$setNoiseOrigin(NoiseOrigin origin);

    double exotic_worlds$getOffsetAt(Context context);
}
