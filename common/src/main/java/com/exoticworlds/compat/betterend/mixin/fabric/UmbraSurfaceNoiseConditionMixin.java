package com.exoticworlds.compat.betterend.mixin.fabric;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.UmbraSurfaceNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.betterend.BetterEndSurfaceNoise;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(UmbraSurfaceNoiseCondition.class)
public class UmbraSurfaceNoiseConditionMixin {
    @Unique
    private static final String GET_DEPTH = "getDepth(III)I";

    @WrapOperation(
            method = GET_DEPTH,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 0))
    private static double toroidal$lapBroadNoise(OpenSimplexNoise noise, double x, double z,
            Operation<Double> original) {
        return BetterEndSurfaceNoise.umbra(noise, x, z, BetterEndSurfaceNoise.UMBRA_BROAD_SCALE, original);
    }

    @WrapOperation(
            method = GET_DEPTH,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 1))
    private static double toroidal$lapFineNoise(OpenSimplexNoise noise, double x, double z,
            Operation<Double> original) {
        return BetterEndSurfaceNoise.umbra(noise, x, z, BetterEndSurfaceNoise.UMBRA_FINE_SCALE, original);
    }
}
