package com.exoticworlds.accessors;

import net.minecraft.world.level.levelgen.DensityFunction;

public interface ScaledShiftedNoise {
    DensityFunction.NoiseHolder exotic_worlds$noise();

    double exotic_worlds$scale();

    boolean exotic_worlds$shiftsScaled();
}
