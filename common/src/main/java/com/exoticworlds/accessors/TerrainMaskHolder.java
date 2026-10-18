package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.TerrainMask;

public interface TerrainMaskHolder {
    @Nullable TerrainMask exotic_worlds$terrainMask();

    void exotic_worlds$terrainMask(TerrainMask mask);
}
