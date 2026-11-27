package com.exoticworlds.accessors;

import net.minecraft.world.level.levelgen.DensityFunction;

public interface ScaledShiftedNoise {
    DensityFunction.NoiseHolder exotic_worlds$getNoise();

    double exotic_worlds$getScale();

    boolean exotic_worlds$isShiftScaled();
}
