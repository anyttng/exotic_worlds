package com.exoticworlds.compat.seedviewer;

import com.exoticworlds.core.WorldFold;

import net.acenia.seedviewer.client.worldgen.NoiseMapType;

public interface FoldedNoiseSampler {
    WorldFold toroidal$fold();

    FoldedSampleCache<Float> toroidal$cache(NoiseMapType type);
}
