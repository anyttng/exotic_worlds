package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import naturaltemperature.NoiseBiomeBandsGenerator;

import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(NoiseBiomeBandsGenerator.class)
public interface NoiseBiomeBandsGeneratorAccessor {
    @Accessor("noise")
    PerlinNoise exotic_worlds$noise();
}
