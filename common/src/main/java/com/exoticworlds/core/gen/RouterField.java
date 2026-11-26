package com.exoticworlds.core.gen;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

public enum RouterField {
    TEMPERATURE,
    VEGETATION,
    CONTINENTS,
    EROSION,
    DEPTH,
    RIDGES,
    PRELIMINARY_SURFACE_LEVEL,
    FINAL_DENSITY;

    public DensityFunction getFunction(NoiseRouter router) {
        return switch (this) {
            case TEMPERATURE -> router.temperature();
            case VEGETATION -> router.vegetation();
            case CONTINENTS -> router.continents();
            case EROSION -> router.erosion();
            case DEPTH -> router.depth();
            case RIDGES -> router.ridges();
            case PRELIMINARY_SURFACE_LEVEL -> router.preliminarySurfaceLevel();
            case FINAL_DENSITY -> router.finalDensity();
        };
    }
}
