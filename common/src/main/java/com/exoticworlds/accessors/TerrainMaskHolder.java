package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.TerrainMask;

public interface TerrainMaskHolder {
    @Nullable TerrainMask toroidal$terrainMask();

    void toroidal$terrainMask(TerrainMask mask);
}
