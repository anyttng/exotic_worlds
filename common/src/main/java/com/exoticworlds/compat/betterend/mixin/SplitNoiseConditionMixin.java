package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.surface.SplitNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.wover.LapNoise;
import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.compat.wover.WoverInjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(SplitNoiseCondition.class)
public class SplitNoiseConditionMixin {
    private static final long SEED = 4141L;

    private static final double SCALE = 0.1;

    @Unique
    private static final OpenSimplexStandIn toroidal$STAND_IN = new OpenSimplexStandIn(SEED);

    @WrapOperation(
            method = {WoverInjectionTargets.GET_NUMBER, "getNoise(II)D", "getNoise(III)D"},
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_2D))
    private double toroidal$lapNoise(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        WorldFold fold = GenerationTransformerContext.context().wrappedTransformer();
        return fold == null
                ? original.call(noise, x, z)
                : LapNoise.eval(toroidal$STAND_IN, fold, x, z, SCALE, SCALE);
    }
}
