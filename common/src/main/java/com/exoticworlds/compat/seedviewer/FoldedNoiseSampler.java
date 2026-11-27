package com.exoticworlds.compat.seedviewer;

import com.exoticworlds.core.fold.WorldFold;

import net.acenia.seedviewer.client.worldgen.NoiseMapType;

public interface FoldedNoiseSampler {
    WorldFold exotic_worlds$getFold();

    FoldedSampleCache<Float> exotic_worlds$getCache(NoiseMapType type);
}
