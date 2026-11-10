package com.exoticworlds.compat.lithostitched.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL;
import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FastNoiseConfig;

@Mixin(FastNoiseConfig.class)
public interface FastNoiseConfigAccessor {
    @Accessor("fnl")
    FNL exotic_worlds$fastNoiseLite();
}
