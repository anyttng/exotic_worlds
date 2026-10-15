package com.exoticworlds.compat.betterend;

import org.betterx.betterend.noise.OpenSimplexNoise;

import com.exoticworlds.compat.wover.LapNoise;
import com.exoticworlds.compat.wover.StandInHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

public final class BetterEndSurfaceNoise {
    public static final double UMBRA_BROAD_SCALE = 0.03;

    public static final double UMBRA_FINE_SCALE = 0.1;

    private static final double SPLIT_SCALE = 0.1;

    public static double split(OpenSimplexNoise noise, double x, double z, Operation<Double> original) {
        return lap(noise, x, z, SPLIT_SCALE, original);
    }

    public static double umbra(OpenSimplexNoise noise, double x, double z, double scale, Operation<Double> original) {
        return lap(noise, x, z, scale, original);
    }

    private static double lap(OpenSimplexNoise noise, double x, double z, double scale, Operation<Double> original) {
        WorldFold fold = GenerationTransformerContext.context().wrappedTransformer();
        return fold == null
                ? original.call(noise, x, z)
                : LapNoise.eval(((StandInHolder) (Object) noise).toroidal$standIn(), fold, x, z, scale, scale);
    }

    private BetterEndSurfaceNoise() {
    }
}
