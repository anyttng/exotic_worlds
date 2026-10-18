package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.SplitNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.betterend.BetterEndSurfaceNoise;
import com.exoticworlds.compat.wover.WoverInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(SplitNoiseCondition.class)
public class SplitNoiseConditionMixin {
    @WrapOperation(
            method = WoverInjectionTargets.GET_NUMBER,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D))
    private double exotic_worlds$lapNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        return BetterEndSurfaceNoise.split(noise, x, z, original);
    }
}
