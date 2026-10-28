package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(value = Climate.Sampler.class, priority = 1100)
public class PaintedFieldMixin {
    @WrapOperation(
            method = {"customTemperatureCalculation(III)F", "customHumidityCalculation(III)F"},
            at = @At(value = "INVOKE", target = InjectionTargets.DENSITY_FUNCTION_COMPUTE))
    private double exotic_worlds$readTheRawFieldWherePainted(DensityFunction field,
            DensityFunction.FunctionContext context, Operation<Double> original) {
        return original.call(field, PinnedSample.at(context));
    }
}
