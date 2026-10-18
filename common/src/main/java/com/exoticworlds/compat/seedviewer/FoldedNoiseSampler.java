package com.exoticworlds.compat.seedviewer;

import com.exoticworlds.core.WorldFold;

import net.acenia.seedviewer.client.worldgen.NoiseMapType;

public interface FoldedNoiseSampler {
    WorldFold exotic_worlds$fold();

    FoldedSampleCache<Float> exotic_worlds$cache(NoiseMapType type);
}
