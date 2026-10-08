package com.exoticworlds.compat.mekanism;

import com.exoticworlds.core.WorldFold;

public interface RadiationBoxFrame {
    void toroidal$setFold(WorldFold fold);

    int toroidal$minX();

    int toroidal$minZ();

    int toroidal$maxX();

    int toroidal$maxZ();
}
