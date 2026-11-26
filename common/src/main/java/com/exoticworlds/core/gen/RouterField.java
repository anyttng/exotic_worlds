package com.exoticworlds.core.gen;

import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

public enum RouterField {
    TEMPERATURE,
    VEGETATION,
    CONTINENTS,
    EROSION,
    DEPTH,
    RIDGES,
    CHUNK_SURFACE_LEVEL,
    FINAL_DENSITY;

    public DensityFunction getFunction(NoiseRouter router) {
        return switch (this) {
            case TEMPERATURE -> router.temperature();
            case VEGETATION -> router.vegetation();
            case CONTINENTS -> router.continents();
            case EROSION -> router.erosion();
            case DEPTH -> router.depth();
            case RIDGES -> router.ridges();
            case CHUNK_SURFACE_LEVEL -> router.chunkSurfaceLevel();
            case FINAL_DENSITY -> router.finalDensity();
        };
    }
}
