package com.exoticworlds.compat.ultraterraforged;

import com.exoticworlds.compat.terraforged.TerraForgedNoise;

import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.function.Interpolation;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Perlin2;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

public enum UltraTerraForgedNoise implements TerraForgedNoise {
    INSTANCE;

    @Override
    public int floor(float value) {
        return NoiseUtil.floor(value);
    }

    @Override
    public float lerp(float from, float to, float alpha) {
        return NoiseUtil.lerp(from, to, alpha);
    }

    @Override
    public float jitterX(int seed, int cellX, int cellZ) {
        return NoiseUtil.cell(seed, cellX, cellZ).x();
    }

    @Override
    public float jitterZ(int seed, int cellX, int cellZ) {
        return NoiseUtil.cell(seed, cellX, cellZ).y();
    }

    @Override
    public float squareSample(float x, float z, int seed) {
        return Perlin2.sample(x, z, seed, Interpolation.CURVE3);
    }

    @Override
    public long pack(int x, int z) {
        return PosUtil.pack(x, z);
    }
}
