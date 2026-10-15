package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.SplitNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.LapNoise;
import com.exoticworlds.compat.StandInHolder;
import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.wover.WoverInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(SplitNoiseCondition.class)
public class SplitNoiseConditionMixin {
    private static final double SCALE = 0.1;

    @WrapOperation(
            method = {WoverInjectionTargets.GET_NUMBER, "getNoise(III)D"},
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D))
    private double toroidal$lapNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        return LapNoise.boundEval(((StandInHolder) (Object) noise).toroidal$standIn(), x, z, SCALE, SCALE,
                () -> original.call(noise, x, z));
    }
}
