package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.terraforged.RtfEntry;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.ultraterraforged.UltraTerraForgedInjectionTargets;
import com.exoticworlds.core.WorldFold;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;

@Mixin(targets = "com.pandaismyname1.ultraterraforged.world.worldgen.densityfunction.NoiseSampler$Sampler",
        remap = false)
public abstract class NoiseSamplerMixin {
    @WrapOperation(method = {"sampleValue", "sampleVolume"},
            at = @At(value = "INVOKE", target = UltraTerraForgedInjectionTargets.NOISE_COMPUTE))
    private float exotic_worlds$bindLap(Noise noise, float x, float z, int seed, Operation<Float> original) {
        WorldFold fold = RtfEntry.generationFold();
        if (fold == null) {
            return original.call(noise, x, z, seed);
        }

        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(fold)) {
            return original.call(noise, RtfEntry.foldX(fold, x, z), RtfEntry.foldZ(fold, z), seed);
        }
    }
}
