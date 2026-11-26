package com.exoticworlds.core.fold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record SeamTransform(int xSign, int zSign, int xShift, int zShift, boolean isSwappingAxes) {
    public static final SeamTransform IDENTITY = new SeamTransform(1, 1, 0, 0);

    public SeamTransform {
        if (Math.abs(xSign) != 1 || Math.abs(zSign) != 1) {
            throw new IllegalArgumentException("An axis sign is +1 or -1, got x=" + xSign + " z=" + zSign);
        }
    }

    public SeamTransform(int xSign, int zSign, int xShift, int zShift) {
        this(xSign, zSign, xShift, zShift, false);
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
        return this.xSign == 1 && this.zSign == 1 && this.xShift == 0 && this.zShift == 0 && !this.isSwappingAxes;
    }

    public FoldOrientation getOrientation() {
        return FoldOrientation.of(this.isSwappingAxes, this.xSign < 0, this.zSign < 0);
    }

    public double applyX(double x, double z) {
        return this.xSign * (this.isSwappingAxes ? z : x) + this.xShift;
    }

    public double applyZ(double x, double z) {
        return this.zSign * (this.isSwappingAxes ? x : z) + this.zShift;
    }

    public int applyCellX(int x, int z) {
        return cellOf(this.xSign, this.xShift, this.isSwappingAxes ? z : x);
    }

    public int applyCellZ(int x, int z) {
        return cellOf(this.zSign, this.zShift, this.isSwappingAxes ? x : z);
    }

    public Vec3 apply(Vec3 pos) {
        if (isIdentity()) {
            return pos;
        }

        return new Vec3(applyX(pos.x, pos.z), pos.y, applyZ(pos.x, pos.z));
    }

    public BlockPos apply(BlockPos pos) {
        if (isIdentity()) {
            return pos;
        }

        return new BlockPos(applyCellX(pos.getX(), pos.getZ()), pos.getY(), applyCellZ(pos.getX(), pos.getZ()));
    }

    public ChunkPos apply(ChunkPos pos) {
        if (isIdentity()) {
            return pos;
        }

        return new ChunkPos(applyCellX(pos.x(), pos.z()), applyCellZ(pos.x(), pos.z()));
    }

    public AABB apply(AABB box) {
        if (isIdentity()) {
            return box;
        }

        return new AABB(
                applyX(box.minX, box.minZ), box.minY, applyZ(box.minX, box.minZ),
                applyX(box.maxX, box.maxZ), box.maxY, applyZ(box.maxX, box.maxZ));
    }

    public BoundingBox apply(BoundingBox box) {
        if (isIdentity()) {
            return box;
        }

        int firstX = applyCellX(box.minX(), box.minZ());
        int secondX = applyCellX(box.maxX(), box.maxZ());
        int firstZ = applyCellZ(box.minX(), box.minZ());
        int secondZ = applyCellZ(box.maxX(), box.maxZ());
        return new BoundingBox(
                Math.min(firstX, secondX), box.minY(), Math.min(firstZ, secondZ),
                Math.max(firstX, secondX), box.maxY(), Math.max(firstZ, secondZ));
    }

    public SeamTransform then(SeamTransform next) {
        int carriedXSign = next.isSwappingAxes ? this.zSign : this.xSign;
        int carriedZSign = next.isSwappingAxes ? this.xSign : this.zSign;
        int carriedXShift = next.isSwappingAxes ? this.zShift : this.xShift;
        int carriedZShift = next.isSwappingAxes ? this.xShift : this.zShift;
        return new SeamTransform(
                carriedXSign * next.xSign,
                carriedZSign * next.zSign,
                next.xSign * carriedXShift + next.xShift,
                next.zSign * carriedZShift + next.zShift,
                this.isSwappingAxes ^ next.isSwappingAxes);
    }

    public SeamTransform inverse() {
        if (this.isSwappingAxes) {
            return new SeamTransform(
                    this.zSign, this.xSign, -this.zSign * this.zShift, -this.xSign * this.xShift, true);
        }

        return new SeamTransform(this.xSign, this.zSign, -this.xSign * this.xShift, -this.zSign * this.zShift);
    }

    public SeamTransform power(int exponent) {
        if (exponent == 0) {
            return IDENTITY;
        }

        if (this.isSwappingAxes) {
            return turnedPower(exponent);
        }

        return new SeamTransform(
                signPower(this.xSign, exponent),
                signPower(this.zSign, exponent),
                shiftPower(this.xSign, this.xShift, exponent),
                shiftPower(this.zSign, this.zShift, exponent));
    }

    private SeamTransform turnedPower(int exponent) {
        if (exponent < 0) {
            return inverse().turnedPower(Math.negateExact(exponent));
        }

        SeamTransform pairs = then(this).power(exponent / 2);
        return (exponent & 1) == 0 ? pairs : pairs.then(this);
    }

    public double powerApplyX(int exponent, double x) {
        return signPower(this.xSign, exponent) * x + shiftPower(this.xSign, this.xShift, exponent);
    }

    public double powerApplyZ(int exponent, double z) {
        return signPower(this.zSign, exponent) * z + shiftPower(this.zSign, this.zShift, exponent);
    }

    public int powerApplyCellX(int exponent, int x) {
        return cellOf(signPower(this.xSign, exponent), shiftPower(this.xSign, this.xShift, exponent), x);
    }

    public int powerApplyCellZ(int exponent, int z) {
        return cellOf(signPower(this.zSign, exponent), shiftPower(this.zSign, this.zShift, exponent), z);
    }

    public int powerSignX(int exponent) {
        return signPower(this.xSign, exponent);
    }

    public int powerSignZ(int exponent) {
        return signPower(this.zSign, exponent);
    }

    public int powerShiftX(int exponent) {
        return shiftPower(this.xSign, this.xShift, exponent);
    }

    public int powerShiftZ(int exponent) {
        return shiftPower(this.zSign, this.zShift, exponent);
    }

    public static int cellOf(int sign, int shift, int cell) {
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
