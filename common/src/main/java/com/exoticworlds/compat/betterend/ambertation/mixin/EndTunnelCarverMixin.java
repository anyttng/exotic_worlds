package com.exoticworlds.compat.betterend.ambertation.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.betterend.TunnelNoiseScales;
import com.exoticworlds.compat.worldgen.LapNoise;
import com.exoticworlds.compat.worldgen.OpenSimplexStandIn;
import com.exoticworlds.compat.worldgen.StandInHolder;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "org.betterx.betterend.world.carvers.EndTunnelCarver")
public class EndTunnelCarverMixin {
    @WrapOperation(
            method = "carve",
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_3D, ordinal = 0))
    private double exotic_worlds$lapHorizontalNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        return fold == null
                ? original.call(noise, x, y, z)
                : LapNoise.eval(exotic_worlds$standIn(noise), fold, x, y, z, TunnelNoiseScales.HORIZONTAL,
                        TunnelNoiseScales.HORIZONTAL);
    }

    @WrapOperation(
            method = "carve",
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 0))
    private double exotic_worlds$lapVerticalNoise(OpenSimplexNoise noise, double x, double z,
            Operation<Double> original) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        return fold == null
                ? original.call(noise, x, z)
                : LapNoise.eval(exotic_worlds$standIn(noise), fold, x, z, TunnelNoiseScales.VERTICAL,
                        TunnelNoiseScales.VERTICAL);
    }

    @WrapOperation(
            method = "carve",
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_3D, ordinal = 1))
    private double exotic_worlds$lapDistortionNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        return fold == null
                ? original.call(noise, x, y, z)
                : LapNoise.eval(exotic_worlds$standIn(noise), fold, x, y, z, TunnelNoiseScales.DISTORTION,
                        TunnelNoiseScales.DISTORTION);
    }

    @Unique
    private static OpenSimplexStandIn exotic_worlds$standIn(OpenSimplexNoise noise) {
        return ((StandInHolder) (Object) noise).exotic_worlds$getStandIn();
    }
}
