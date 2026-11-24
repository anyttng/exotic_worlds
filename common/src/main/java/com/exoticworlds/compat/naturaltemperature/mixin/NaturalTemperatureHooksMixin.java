package com.exoticworlds.compat.naturaltemperature.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.naturaltemperature.ClimateLap;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.naturaltemperature.NaturalTemperatureHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

@Mixin(NaturalTemperatureHooks.class)
public class NaturalTemperatureHooksMixin {
    @WrapOperation(
            method = "getLatitudeAt",
            at = @At(value = "INVOKE", target = "Lcom/naturaltemperature/LatitudeCalculator;calculate(II)D"))
    private static double exotic_worlds$latitudeOnTheLap(int x, int z, Operation<Double> original) {
        ClimateLap lap = ClimateLap.ofTheOverworld();
        return lap == null ? original.call(x, z) : original.call(lap.x(x, z), lap.z(z));
    }

    @WrapOperation(
            method = "getTemperatureAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/densityfunction/DensitySampler$Bound;"
                            + "sampleValue(III)F"))
    private static float exotic_worlds$temperatureOnTheLap(DensitySampler.Bound temperature, int x, int y, int z,
            Operation<Float> original) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(CurrentServer.get(), Level.OVERWORLD);
        if (fold == null) {
            return original.call(temperature, x, y, z);
        }

        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(fold)) {
            return original.call(temperature, x, y, z);
        }
    }
}
