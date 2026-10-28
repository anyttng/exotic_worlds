package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.naturaltemperature.ClimateLap;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.naturaltemperature.NoiseBiomeBandsGenerator;
import com.naturaltemperature.Painter.CustomTemperatureSampler;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

@Mixin(CustomTemperatureSampler.class)
public class CustomTemperatureSamplerMixin {
    @WrapOperation(
            method = "sampleValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/naturaltemperature/Painter/PainterCore;calculateTemperature("
                            + "Lnet/minecraft/world/level/levelgen/densityfunction/DensitySampler$Bound;"
                            + "Lcom/naturaltemperature/NoiseBiomeBandsGenerator;III)F"))
    private float exotic_worlds$paintTheLap(DensitySampler.Bound rawTemperature, NoiseBiomeBandsGenerator noise,
            int x, int y, int z, Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(rawTemperature, noise, x, y, z);
        }

        return original.call(PinnedSample.at(rawTemperature, x, y, z), noise, lap.x(x, z), y, lap.z(z));
    }
}
