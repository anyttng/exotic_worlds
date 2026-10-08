package com.exoticworlds.compat.seedviewer;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

public interface FoldedNoiseSampler {
    WorldFold toroidal$fold();

    @Nullable
    FoldedSampleCache<Float> toroidal$cache(String layer);
}
