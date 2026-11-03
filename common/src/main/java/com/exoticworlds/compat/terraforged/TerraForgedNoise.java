package com.exoticworlds.compat.terraforged;

public interface TerraForgedNoise {
    int floor(float value);

    float lerp(float from, float to, float alpha);

    float jitterX(int seed, int cellX, int cellZ);

    float jitterZ(int seed, int cellX, int cellZ);

    float squareSample(float x, float z, int seed);

    long pack(int x, int z);
}
