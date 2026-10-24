package com.exoticworlds.compat.ultraterraforged;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.Interpolation;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Perlin2;

public final class SimplexOnSquare {
    public static final double RATE = 1.777;

    public static final float SIMPLEX_AMPLITUDE = 2.050F;

    public static final float SIMPLEX2_AMPLITUDE = 2.562F;

    static final Interpolation CURVE = Interpolation.CURVE3;

    public static float sample(float x, float z, int seed, float amplitude) {
        return amplitude * Perlin2.sample(x, z, seed, CURVE);
    }

    public static float simplex(float x, float z, int seed, Interpolation unused) {
        return sample(x, z, seed, SIMPLEX_AMPLITUDE);
    }

    public static float simplex2(float x, float z, int seed, Interpolation unused) {
        return sample(x, z, seed, SIMPLEX2_AMPLITUDE);
    }

    private SimplexOnSquare() {
    }
}
