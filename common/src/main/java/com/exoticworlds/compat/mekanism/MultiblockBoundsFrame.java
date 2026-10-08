package com.exoticworlds.compat.mekanism;

import com.exoticworlds.core.WorldFold;

public interface MultiblockBoundsFrame {
    WorldFold toroidal$fold();

    void toroidal$setFold(WorldFold fold);
}
