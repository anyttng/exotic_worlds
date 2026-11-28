package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.levelgen.DensityFunction;

public interface InterpretedDensity {
    @Nullable
    DensityFunction exotic_worlds$getInterpreted();
}
