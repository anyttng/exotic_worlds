package com.exoticworlds.compat.betterend.betterx.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.features.terrain.caves.TunelCaveFeature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.worldgen.LapNoise;
import com.exoticworlds.compat.worldgen.OpenSimplexStandIn;
import com.exoticworlds.compat.worldgen.StandInHolder;
import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.betterend.TunnelNoiseScales;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.WorldGenLevel;

@Mixin(TunelCaveFeature.class)
public class TunelCaveFeatureMixin {
    @Unique
    private static final String CARVE_COLUMN = "lambda$generate$0";

    @WrapOperation(
            method = CARVE_COLUMN,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_3D, ordinal = 0))
    private double exotic_worlds$lapHorizontalNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original, @Local(argsOnly = true) WorldGenLevel world) {
        WorldFold fold = exotic_worlds$getFold(world);
        return fold == null
                ? original.call(noise, x, y, z)
                : LapNoise.eval(exotic_worlds$standIn(noise), fold, x, y, z, TunnelNoiseScales.HORIZONTAL,
                        TunnelNoiseScales.HORIZONTAL);
    }

    @WrapOperation(
            method = CARVE_COLUMN,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 0))
    private double exotic_worlds$lapVerticalNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original,
            @Local(argsOnly = true) WorldGenLevel world) {
        WorldFold fold = exotic_worlds$getFold(world);
        return fold == null
                ? original.call(noise, x, z)
                : LapNoise.eval(exotic_worlds$standIn(noise), fold, x, z, TunnelNoiseScales.VERTICAL,
                        TunnelNoiseScales.VERTICAL);
    }

    @WrapOperation(
            method = CARVE_COLUMN,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_3D, ordinal = 1))
    private double exotic_worlds$lapDistortionNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original, @Local(argsOnly = true) WorldGenLevel world) {
        WorldFold fold = exotic_worlds$getFold(world);
        return fold == null
                ? original.call(noise, x, y, z)
                : LapNoise.eval(exotic_worlds$standIn(noise), fold, x, y, z, TunnelNoiseScales.DISTORTION,
                        TunnelNoiseScales.DISTORTION);
    }

    // The columns run on ForkJoin threads, where GenerationFoldContext holds no fold.
    @Unique
    private static @Nullable WorldFold exotic_worlds$getFold(WorldGenLevel world) {
        return WorldLoopAttachments.getWrappedFold(world.getLevel());
    }

    @Unique
    private static OpenSimplexStandIn exotic_worlds$standIn(OpenSimplexNoise noise) {
        return ((StandInHolder) (Object) noise).exotic_worlds$getStandIn();
    }
}
