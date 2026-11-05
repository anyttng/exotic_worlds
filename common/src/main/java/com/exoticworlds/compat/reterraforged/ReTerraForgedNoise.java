package com.exoticworlds.compat.reterraforged;

import com.exoticworlds.compat.terraforged.TerraForgedNoise;

import raccoonman.reterraforged.world.worldgen.noise.function.Interpolation;
import raccoonman.reterraforged.world.worldgen.noise.module.Perlin2;

public enum ReTerraForgedNoise implements TerraForgedNoise {
    INSTANCE;

    @Override
    public float squareSample(float x, float z, int seed) {
        return Perlin2.sample(x, z, seed, Interpolation.CURVE3);
    }
}
