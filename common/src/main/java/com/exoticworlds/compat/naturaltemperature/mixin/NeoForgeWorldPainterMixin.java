package com.exoticworlds.compat.naturaltemperature.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.exoticworlds.compat.naturaltemperature.ClimateLap;
import com.exoticworlds.compat.naturaltemperature.NaturalTemperatureInjectionTargets;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.naturaltemperature.Config;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(value = Climate.Sampler.class, priority = 1100)
public class NeoForgeWorldPainterMixin {
    private static final String WORLD_PAINTER = "com.naturaltemperature.mixin.WorldPainter";

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.TEMPERATURE_CALCULATION))
    @TargetHandler(mixin = WORLD_PAINTER, name = NaturalTemperatureInjectionTargets.PAINT)
    private float exotic_worlds$paintTheLapTemperature(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        DensityFunction.@Nullable FunctionContext previous = PinnedSample.enter(x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y,
                    lap.z(z, Config.generation_mode, Config.equatorial_distance, Config.equator_offset));
        } finally {
            PinnedSample.exit(previous);
        }
    }

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = NaturalTemperatureInjectionTargets.HUMIDITY_CALCULATION))
    @TargetHandler(mixin = WORLD_PAINTER, name = NaturalTemperatureInjectionTargets.PAINT)
    private float exotic_worlds$paintTheLapHumidity(Climate.Sampler sampler, int x, int y, int z,
            Operation<Float> original) {
        ClimateLap lap = ClimateLap.bound();
        if (lap == null) {
            return original.call(sampler, x, y, z);
        }

        DensityFunction.@Nullable FunctionContext previous = PinnedSample.enter(x, y, z);
        try {
            return original.call(sampler, lap.x(x, z), y,
                    lap.z(z, Config.generation_mode, Config.equatorial_distance, Config.equator_offset));
        } finally {
            PinnedSample.exit(previous);
        }
    }
}
