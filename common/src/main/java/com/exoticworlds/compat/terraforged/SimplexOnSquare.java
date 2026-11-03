package com.exoticworlds.compat.terraforged;

public final class SimplexOnSquare {
    public static final double RATE = 1.777;

    public static final float SIMPLEX_AMPLITUDE = 2.050F;

    public static final float SIMPLEX2_AMPLITUDE = 2.562F;

    public static float sample(TerraForgedNoise noise, float x, float z, int seed, float amplitude) {
        return amplitude * noise.squareSample(x, z, seed);
    }

    public static float simplex(TerraForgedNoise noise, float x, float z, int seed) {
        return sample(noise, x, z, seed, SIMPLEX_AMPLITUDE);
    }

    public static float simplex2(TerraForgedNoise noise, float x, float z, int seed) {
        return sample(noise, x, z, seed, SIMPLEX2_AMPLITUDE);
    }

    private SimplexOnSquare() {
    }
}
