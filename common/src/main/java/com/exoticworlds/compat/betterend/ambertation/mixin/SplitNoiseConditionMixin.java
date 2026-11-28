package com.exoticworlds.compat.betterend.ambertation.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.SplitNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.worldgen.LapNoise;
import com.exoticworlds.compat.worldgen.StandInHolder;
import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.wover.WoverInjectionTargets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(SplitNoiseCondition.class)
public class SplitNoiseConditionMixin {
    @Unique
    private static final double SCALE = 0.1;

    @WrapOperation(
            method = {WoverInjectionTargets.SURFACE_RULES_GET_NUMBER, "getNoise(III)D"},
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D))
    private double exotic_worlds$lapNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        return fold == null
                ? original.call(noise, x, z)
                : LapNoise.eval(((StandInHolder) (Object) noise).exotic_worlds$getStandIn(), fold, x, z, SCALE, SCALE);
    }
}
