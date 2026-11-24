package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import naturaltemperature.MathUtils;

@Mixin(MathUtils.class)
public interface MathUtilsAccessor {
    @Invoker("pointPosition")
    static double exotic_worlds$pointPosition(long cell, double spacing, long seed) {
        throw new AssertionError();
    }

    @Invoker("random01")
    static double exotic_worlds$random01(long seed, long index, long salt) {
        throw new AssertionError();
    }

    @Accessor("VALUE_SALT")
    static long exotic_worlds$getValueSalt() {
        throw new AssertionError();
    }
}
