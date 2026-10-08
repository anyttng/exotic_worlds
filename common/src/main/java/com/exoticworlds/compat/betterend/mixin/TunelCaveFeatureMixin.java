package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.features.terrain.caves.TunelCaveFeature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.wover.LapNoise;
import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.compat.wover.StandInHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.WorldGenLevel;

@Mixin(TunelCaveFeature.class)
public class TunelCaveFeatureMixin {
    private static final String CARVE_COLUMN = "lambda$generate$0";

    private static final double HORIZONTAL_SCALE = 0.02;

    private static final double VERTICAL_SCALE = 0.01;

    private static final double DISTORTION_SCALE = 0.1;

    @WrapOperation(
            method = CARVE_COLUMN,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_3D, ordinal = 0))
    private double toroidal$lapHorizontalNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original, @Local(argsOnly = true) WorldGenLevel world) {
        WorldFold fold = toroidal$fold(world);
        return fold == null
                ? original.call(noise, x, y, z)
                : LapNoise.eval(toroidal$standIn(noise), fold, x, y, z, HORIZONTAL_SCALE, HORIZONTAL_SCALE);
    }

    @WrapOperation(
            method = CARVE_COLUMN,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 0))
    private double toroidal$lapVerticalNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original,
            @Local(argsOnly = true) WorldGenLevel world) {
        WorldFold fold = toroidal$fold(world);
        return fold == null
                ? original.call(noise, x, z)
                : LapNoise.eval(toroidal$standIn(noise), fold, x, z, VERTICAL_SCALE, VERTICAL_SCALE);
    }

    @WrapOperation(
            method = CARVE_COLUMN,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_3D, ordinal = 1))
    private double toroidal$lapDistortionNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original, @Local(argsOnly = true) WorldGenLevel world) {
        WorldFold fold = toroidal$fold(world);
        return fold == null
                ? original.call(noise, x, y, z)
                : LapNoise.eval(toroidal$standIn(noise), fold, x, y, z, DISTORTION_SCALE, DISTORTION_SCALE);
    }

    // The columns run on ForkJoin threads, where GenerationTransformerContext holds no fold.
    @Unique
    private static @Nullable WorldFold toroidal$fold(WorldGenLevel world) {
        return WorldLoopAttachments.wrappedTransformerOf(world.getLevel());
    }

    @Unique
    private static OpenSimplexStandIn toroidal$standIn(OpenSimplexNoise noise) {
        return ((StandInHolder) (Object) noise).toroidal$standIn();
    }
}
