package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.UmbraSurfaceNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.wover.LapNoise;
import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(UmbraSurfaceNoiseCondition.class)
public class UmbraSurfaceNoiseConditionMixin {
    private static final String GET_DEPTH = "getDepth(III)I";

    private static final long SEED = 1512L;

    private static final double BROAD_SCALE = 0.03;

    private static final double FINE_SCALE = 0.1;

    @Unique
    private static final OpenSimplexStandIn toroidal$STAND_IN = new OpenSimplexStandIn(SEED);

    @WrapOperation(
            method = GET_DEPTH,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 0))
    private static double toroidal$lapBroadNoise(OpenSimplexNoise noise, double x, double z,
            Operation<Double> original) {
        return toroidal$lapNoise(noise, x, z, BROAD_SCALE, original);
    }

    @WrapOperation(
            method = GET_DEPTH,
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D, ordinal = 1))
    private static double toroidal$lapFineNoise(OpenSimplexNoise noise, double x, double z,
            Operation<Double> original) {
        return toroidal$lapNoise(noise, x, z, FINE_SCALE, original);
    }

    @Unique
    private static double toroidal$lapNoise(OpenSimplexNoise noise, double x, double z, double scale,
            Operation<Double> original) {
        WorldFold fold = GenerationTransformerContext.context().wrappedTransformer();
        return fold == null
                ? original.call(noise, x, z)
                : LapNoise.eval(toroidal$STAND_IN, fold, x, z, scale, scale);
    }
}
