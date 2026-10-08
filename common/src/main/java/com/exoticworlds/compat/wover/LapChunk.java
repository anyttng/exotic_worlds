package com.exoticworlds.compat.wover;

interface LapChunk<T> {
    int sideX();

    int sideZ();

    T get(int x, int z);

    void set(int x, int z, T biome);
}
