package com.exoticworlds.compat.wover;

import net.minecraft.world.level.levelgen.WorldgenRandom;

final class SquareLapChunk<T> implements LapChunk<T> {
    private static final int PRE_CAPACITY = 64;

    private static final int PRE_BIT_OFFSET = 2;

    private static final int OFFSET_ODDS = 2;

    private final int sideX;

    private final int sideZ;

    private final Object[] cells;

    SquareLapChunk(int sideX, int sideZ, WorldgenRandom random, LapPicker<T> picker) {
        this.sideX = sideX;
        this.sideZ = sideZ;
        int preX = (sideX + 1) / 2;
        int preZ = (sideZ + 1) / 2;
        Object[] pre = new Object[PRE_CAPACITY];
        for (int x = 0; x < preX; x++) {
            for (int z = 0; z < preZ; z++) {
                pre[preIndex(x, z)] = picker.pick().apply(random);
            }
        }

        this.cells = new Object[sideX * sideZ];
        for (int x = 0; x < sideX; x++) {
            for (int z = 0; z < sideZ; z++) {
                int px = offset(x, preX, random);
                int pz = offset(z, preZ, random);
                @SuppressWarnings("unchecked")
                T parent = (T) pre[preIndex(px, pz)];
                this.cells[index(x, z)] = picker.subBiome().apply(parent, random);
            }
        }
    }

    @Override
    public int sideX() {
        return this.sideX;
    }

    @Override
    public int sideZ() {
        return this.sideZ;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int x, int z) {
        return (T) this.cells[index(x, z)];
    }

    @Override
    public void set(int x, int z, T biome) {
        this.cells[index(x, z)] = biome;
    }

    private static int offset(int cell, int preSide, WorldgenRandom random) {
        return ((cell + random.nextInt(OFFSET_ODDS)) >> 1) % preSide;
    }

    private static int preIndex(int x, int z) {
        return x << PRE_BIT_OFFSET | z;
    }

    private int index(int x, int z) {
        return x * this.sideZ + z;
    }
}
