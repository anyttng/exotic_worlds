package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.UmbraSurfaceNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.LapNoise;
import com.exoticworlds.compat.StandInHolder;
import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(UmbraSurfaceNoiseCondition.class)
public class UmbraSurfaceNoiseConditionMixin {
    @Unique
    private static final String GET_DEPTH = "getDepth(III)I";

    @Unique
    private static final double BROAD_SCALE = 0.03;

    @Unique
    private static final double FINE_SCALE = 0.1;

    @WrapOperation(
            method = GET_DEPTH,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 0))
    private static double exotic_worlds$lapBroadNoise(OpenSimplexNoise noise, double x, double z,
            Operation<Double> original) {
        return LapNoise.boundEval(((StandInHolder) (Object) noise).exotic_worlds$standIn(), x, z, BROAD_SCALE, BROAD_SCALE,
                () -> original.call(noise, x, z));
    }

    @WrapOperation(
            method = GET_DEPTH,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 1))
    private static double exotic_worlds$lapFineNoise(OpenSimplexNoise noise, double x, double z,
            Operation<Double> original) {
        return LapNoise.boundEval(((StandInHolder) (Object) noise).exotic_worlds$standIn(), x, z, FINE_SCALE, FINE_SCALE,
                () -> original.call(noise, x, z));
    }
}
