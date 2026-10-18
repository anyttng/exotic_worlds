package com.exoticworlds.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record SeamTransform(int xSign, int zSign, int xShift, int zShift) {
    public static final SeamTransform IDENTITY = new SeamTransform(1, 1, 0, 0);

    public SeamTransform {
        if (Math.abs(xSign) != 1 || Math.abs(zSign) != 1) {
            throw new IllegalArgumentException("An axis sign is +1 or -1, got x=" + xSign + " z=" + zSign);
        }
    }

    public static SeamTransform translation(int xShift, int zShift) {
        return new SeamTransform(1, 1, xShift, zShift);
    }

    public static SeamTransform glideX(int xShift, int zShift) {
        return new SeamTransform(1, -1, xShift, zShift);
    }

    public static SeamTransform glideZ(int xShift, int zShift) {
        return new SeamTransform(-1, 1, xShift, zShift);
    }

    public boolean isIdentity() {
        return this.xSign == 1 && this.zSign == 1 && this.xShift == 0 && this.zShift == 0;
    }

    public FoldOrientation orientation() {
        return FoldOrientation.of(this.xSign < 0, this.zSign < 0);
    }

    public double applyX(double x) {
        return this.xSign * x + this.xShift;
    }

    public double applyZ(double z) {
        return this.zSign * z + this.zShift;
    }

    public int applyCellX(int x) {
        return cellOf(this.xSign, this.xShift, x);
    }

    public int applyCellZ(int z) {
        return cellOf(this.zSign, this.zShift, z);
    }

    public Vec3 apply(Vec3 pos) {
        if (isIdentity()) {
            return pos;
        }

        return new Vec3(applyX(pos.x), pos.y, applyZ(pos.z));
    }

    public BlockPos apply(BlockPos pos) {
        if (isIdentity()) {
            return pos;
        }

        return new BlockPos(applyCellX(pos.getX()), pos.getY(), applyCellZ(pos.getZ()));
    }

    public ChunkPos apply(ChunkPos pos) {
        if (isIdentity()) {
            return pos;
        }

        return new ChunkPos(applyCellX(pos.x), applyCellZ(pos.z));
    }

    public AABB apply(AABB box) {
        if (isIdentity()) {
            return box;
        }

        return new AABB(applyX(box.minX), box.minY, applyZ(box.minZ), applyX(box.maxX), box.maxY, applyZ(box.maxZ));
    }

    public BoundingBox apply(BoundingBox box) {
        if (isIdentity()) {
            return box;
        }

        int firstX = applyCellX(box.minX());
        int secondX = applyCellX(box.maxX());
        int firstZ = applyCellZ(box.minZ());
        int secondZ = applyCellZ(box.maxZ());
        return new BoundingBox(
                Math.min(firstX, secondX), box.minY(), Math.min(firstZ, secondZ),
                Math.max(firstX, secondX), box.maxY(), Math.max(firstZ, secondZ));
    }

    public SeamTransform then(SeamTransform next) {
        return new SeamTransform(
                this.xSign * next.xSign,
                this.zSign * next.zSign,
                next.xSign * this.xShift + next.xShift,
                next.zSign * this.zShift + next.zShift);
    }

    public SeamTransform inverse() {
        return new SeamTransform(this.xSign, this.zSign, -this.xSign * this.xShift, -this.zSign * this.zShift);
    }

    public SeamTransform power(int exponent) {
        if (exponent == 0) {
            return IDENTITY;
        }

        return new SeamTransform(
                signPower(this.xSign, exponent),
                signPower(this.zSign, exponent),
                shiftPower(this.xSign, this.xShift, exponent),
                shiftPower(this.zSign, this.zShift, exponent));
    }

    double powerApplyX(int exponent, double x) {
        return signPower(this.xSign, exponent) * x + shiftPower(this.xSign, this.xShift, exponent);
    }

    double powerApplyZ(int exponent, double z) {
        return signPower(this.zSign, exponent) * z + shiftPower(this.zSign, this.zShift, exponent);
    }

    int powerApplyCellX(int exponent, int x) {
        return cellOf(signPower(this.xSign, exponent), shiftPower(this.xSign, this.xShift, exponent), x);
    }

    int powerApplyCellZ(int exponent, int z) {
        return cellOf(signPower(this.zSign, exponent), shiftPower(this.zSign, this.zShift, exponent), z);
    }

    int powerSignX(int exponent) {
        return signPower(this.xSign, exponent);
    }

    int powerSignZ(int exponent) {
        return signPower(this.zSign, exponent);
    }

    int powerShiftX(int exponent) {
        return shiftPower(this.xSign, this.xShift, exponent);
    }

    int powerShiftZ(int exponent) {
        return shiftPower(this.zSign, this.zShift, exponent);
    }

    static int cellOf(int sign, int shift, int cell) {
        return sign * cell + shift + (sign < 0 ? -1 : 0);
    }

    private static int signPower(int sign, int exponent) {
        return sign > 0 || (exponent & 1) == 0 ? 1 : -1;
    }

    private static int shiftPower(int sign, int shift, int exponent) {
        if (sign > 0) {
            return Math.multiplyExact(shift, exponent);
        }

        return (exponent & 1) == 0 ? 0 : shift;
    }
}
