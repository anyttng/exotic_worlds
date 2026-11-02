package com.exoticworlds.compat.betterend;

import org.betterx.betterend.noise.OpenSimplexNoise;

import com.exoticworlds.compat.LapNoise;
import com.exoticworlds.compat.StandInHolder;
import com.exoticworlds.core.WorldFold;

public final class BetterEndSurfaceNoise {
    public static final double UMBRA_BROAD_SCALE = 0.03;

    public static final double UMBRA_FINE_SCALE = 0.1;

    private static final double SPLIT_SCALE = 0.1;

    public static double split(OpenSimplexNoise noise, WorldFold fold, double x, double z) {
        return lap(noise, fold, x, z, SPLIT_SCALE);
    }

    public static double umbra(OpenSimplexNoise noise, WorldFold fold, double x, double z, double scale) {
        return lap(noise, fold, x, z, scale);
    }

    private static double lap(OpenSimplexNoise noise, WorldFold fold, double x, double z, double scale) {
        return LapNoise.eval(((StandInHolder) (Object) noise).exotic_worlds$standIn(), fold, x, z, scale, scale);
    }

    private BetterEndSurfaceNoise() {
    }
}
