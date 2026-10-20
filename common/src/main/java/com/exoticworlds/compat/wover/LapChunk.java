package com.exoticworlds.compat.wover;

class LapChunk<T> {
    private final int sideX;

    private final int sideZ;

    final Object[] cells;

    LapChunk(int sideX, int sideZ) {
        this.sideX = sideX;
        this.sideZ = sideZ;
        this.cells = new Object[sideX * sideZ];
    }

    int sideX() {
        return this.sideX;
    }

    int sideZ() {
        return this.sideZ;
    }

    @SuppressWarnings("unchecked")
    T get(int x, int z) {
        return (T) this.cells[index(x, z)];
    }

    void set(int x, int z, T biome) {
        this.cells[index(x, z)] = biome;
    }

    final int index(int x, int z) {
        return x * this.sideZ + z;
    }
}
