package com.exoticworlds.accessors;

import com.exoticworlds.core.WorldFold;

public interface FoldedClimateSampler {
    WorldFold toroidal$fold();

    void toroidal$fold(WorldFold fold);
}
