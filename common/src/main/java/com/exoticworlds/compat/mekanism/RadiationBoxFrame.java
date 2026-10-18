package com.exoticworlds.compat.mekanism;

import com.exoticworlds.core.WorldFold;

public interface RadiationBoxFrame {
    void exotic_worlds$setFold(WorldFold fold);

    int exotic_worlds$minX();

    int exotic_worlds$minZ();

    int exotic_worlds$maxX();

    int exotic_worlds$maxZ();
}
