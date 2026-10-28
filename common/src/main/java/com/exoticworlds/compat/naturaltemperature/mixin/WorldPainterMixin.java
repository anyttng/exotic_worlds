package com.exoticworlds.compat.naturaltemperature.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.naturaltemperature.ClimateLap;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(value = Climate.Sampler.class, priority = 1100)
public class WorldPainterMixin {
    private static final String WORLD_PAINTER = "naturaltemperature.mixin.WorldPainter";

    private static final String PAINT = "modifyTemperature";

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;customTemperatureCalculation(III)F"))
    @TargetHandler(mixin = WORLD_PAINTER, name = PAINT)
    private float exotic_worlds$paintTheLapTemperature(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        DensityFunction.@Nullable FunctionContext previous = PinnedSample.enter(x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y, lap.z(z));
        } finally {
            PinnedSample.exit(previous);
        }
    }

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Climate$Sampler;customHumidityCalculation(III)F"))
    @TargetHandler(mixin = WORLD_PAINTER, name = PAINT)
    private float exotic_worlds$paintTheLapHumidity(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        DensityFunction.@Nullable FunctionContext previous = PinnedSample.enter(x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y, lap.z(z));
        } finally {
            PinnedSample.exit(previous);
        }
    }

    @WrapOperation(
            method = {"customTemperatureCalculation(III)F", "customHumidityCalculation(III)F"},
            at = @At(value = "INVOKE", target = InjectionTargets.DENSITY_FUNCTION_COMPUTE))
    private double exotic_worlds$readTheRawFieldWherePainted(DensityFunction field,
            DensityFunction.FunctionContext context, Operation<Double> original) {
        return original.call(field, PinnedSample.at(context));
    }
}
