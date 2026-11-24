package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.naturaltemperature.ClimateLap;
import com.exoticworlds.compat.naturaltemperature.PinnedSample;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import naturaltemperature.NaturalTemperatureHooks;
import naturaltemperature.TemperatureContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(NaturalTemperatureHooks.class)
public class NaturalTemperatureHooksMixin {
    private static final String TEMPERATURE_AT = "getTemperatureAt";

    @WrapOperation(
            method = "getLatitudeAt",
            at = @At(value = "INVOKE", target = "Lnaturaltemperature/LatitudeCalculator;calculate(II)D"))
    private static double exotic_worlds$latitudeOnTheLap(int x, int z, Operation<Double> original) {
        ClimateLap lap = ClimateLap.ofTheOverworld();
        return lap == null ? original.call(x, z) : original.call(lap.x(x, z), lap.z(z));
    }

    @WrapOperation(
            method = TEMPERATURE_AT,
            at = @At(value = "INVOKE", target = InjectionTargets.DENSITY_FUNCTION_COMPUTE))
    private static double exotic_worlds$rawTemperatureOnTheLap(DensityFunction temperature,
            DensityFunction.FunctionContext context, Operation<Double> original) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(CurrentServer.get(), Level.OVERWORLD);
        if (fold == null) {
            return original.call(temperature, context);
        }

        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(fold)) {
            return original.call(temperature, context);
        }
    }

    @WrapOperation(
            method = TEMPERATURE_AT,
            at = @At(
                    value = "INVOKE",
                    target = "Lnaturaltemperature/TemperatureContextFactory;create(IIID)"
                            + "Lnaturaltemperature/TemperatureContext;"))
    private static TemperatureContext exotic_worlds$temperatureOnTheLap(int x, int y, int z, double rawTemperature,
            Operation<TemperatureContext> original) {
        ClimateLap lap = ClimateLap.ofTheOverworld();
        if (lap == null) {
            return original.call(x, y, z, rawTemperature);
        }

        PinnedSample previous = PinnedSample.enter(lap, x, y, z);
        try {
            return original.call(lap.x(x, z), y, lap.z(z), rawTemperature);
        } finally {
            PinnedSample.exit(previous);
        }
    }
}
