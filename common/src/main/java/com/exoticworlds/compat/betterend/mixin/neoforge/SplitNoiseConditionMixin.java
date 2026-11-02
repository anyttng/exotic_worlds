package com.exoticworlds.compat.betterend.mixin.neoforge;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.SplitNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.betterend.BetterEndSurfaceNoise;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(SplitNoiseCondition.class)
public class SplitNoiseConditionMixin {
    @WrapOperation(method = "getNoise(II)D", at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D))
    private double exotic_worlds$lapNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        return fold == null
                ? original.call(noise, x, z)
                : BetterEndSurfaceNoise.split(noise, fold, x, z);
    }
}
