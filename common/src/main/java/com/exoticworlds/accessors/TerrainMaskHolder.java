package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.terrain.TerrainMask;

public interface TerrainMaskHolder {
    @Nullable TerrainMask exotic_worlds$getTerrainMask();

    void exotic_worlds$setTerrainMask(TerrainMask mask);
}
