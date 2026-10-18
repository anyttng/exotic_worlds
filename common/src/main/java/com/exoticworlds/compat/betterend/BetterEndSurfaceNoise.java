package com.exoticworlds.compat.betterend;

import org.betterx.betterend.noise.OpenSimplexNoise;

import com.exoticworlds.compat.LapNoise;
import com.exoticworlds.compat.StandInHolder;
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
        return LapNoise.boundEval(((StandInHolder) (Object) noise).exotic_worlds$standIn(), x, z, scale, scale,
                () -> original.call(noise, x, z));
    }

    private BetterEndSurfaceNoise() {
    }
}
