package com.exoticworlds.compat.ultraterraforged;

import com.exoticworlds.compat.terraforged.TerraForgedNoise;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.Interpolation;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Perlin2;

public enum UltraTerraForgedNoise implements TerraForgedNoise {
    INSTANCE;

    @Override
    public float squareSample(float x, float z, int seed) {
        return Perlin2.sample(x, z, seed, Interpolation.CURVE3);
    }
}
