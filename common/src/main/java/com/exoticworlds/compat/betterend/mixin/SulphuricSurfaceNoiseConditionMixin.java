package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.SulphuricSurfaceNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.LapNoise;
import com.exoticworlds.compat.StandInHolder;
import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.wover.WoverInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(SulphuricSurfaceNoiseCondition.class)
public class SulphuricSurfaceNoiseConditionMixin {
    @Unique
    private static final double BROAD_SCALE = 0.03;

    @Unique
    private static final double FINE_SCALE = 0.1;

    @WrapOperation(
            method = WoverInjectionTargets.GET_NUMBER,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 0))
    private double toroidal$lapBroadNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        return LapNoise.boundEval(((StandInHolder) (Object) noise).toroidal$standIn(), x, z, BROAD_SCALE, BROAD_SCALE,
                () -> original.call(noise, x, z));
    }

    @WrapOperation(
            method = WoverInjectionTargets.GET_NUMBER,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 1))
    private double toroidal$lapFineNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        return LapNoise.boundEval(((StandInHolder) (Object) noise).toroidal$standIn(), x, z, FINE_SCALE, FINE_SCALE,
                () -> original.call(noise, x, z));
    }
}
