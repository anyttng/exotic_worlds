package com.exoticworlds.core;

public record CuboidFace(Side side, int minBlockX, int minBlockZ, int widthBlocks, int depthBlocks,
        BoxVector origin, BoxVector alongX, BoxVector alongZ) {
    public enum Side {
        WEST,
        FRONT,
        EAST,
        BACK,
        TOP,
        BOTTOM
    }

    public record BoxVector(int a, int b, int c) {
    }

    public boolean contains(int blockX, int blockZ) {
        int localX = localX(blockX);
        int localZ = localZ(blockZ);
        return localX >= 0 && localX < this.widthBlocks && localZ >= 0 && localZ < this.depthBlocks;
    }

    public int localX(int blockX) {
        return blockX - this.minBlockX;
    }

    public int localZ(int blockZ) {
        return blockZ - this.minBlockZ;
    }

    public int blockX(int localX) {
        return this.minBlockX + localX;
    }

    public int blockZ(int localZ) {
        return this.minBlockZ + localZ;
    }

    public BoxVector boxPoint(int localX, int localZ) {
        return new BoxVector(
                this.origin.a() + localX * this.alongX.a() + localZ * this.alongZ.a(),
                this.origin.b() + localX * this.alongX.b() + localZ * this.alongZ.b(),
                this.origin.c() + localX * this.alongX.c() + localZ * this.alongZ.c());
    }
}
