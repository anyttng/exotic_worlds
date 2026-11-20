package com.exoticworlds.core;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public enum FoldOrientation {
    IDENTITY(false, false, false),
    MIRROR_X(false, true, false),
    MIRROR_Z(false, false, true),
    HALF_TURN(false, true, true),
    QUARTER_TURN_CLOCKWISE(true, true, false),
    QUARTER_TURN_COUNTERCLOCKWISE(true, false, true),
    MIRROR_DIAGONAL(true, false, false),
    MIRROR_ANTIDIAGONAL(true, true, true);

    private static final FoldOrientation[] BY_PARTS = {
            IDENTITY, MIRROR_Z, MIRROR_X, HALF_TURN,
            MIRROR_DIAGONAL, QUARTER_TURN_COUNTERCLOCKWISE, QUARTER_TURN_CLOCKWISE, MIRROR_ANTIDIAGONAL};

    private static final int SWAP_BIT = 4;
    private static final int FLIP_X_BIT = 2;
    private static final int FLIP_Z_BIT = 1;

    private static final double BLOCK_WIDTH = 1.0;

    private final boolean swapsAxes;
    private final boolean flipsX;
    private final boolean flipsZ;

    FoldOrientation(boolean swapsAxes, boolean flipsX, boolean flipsZ) {
        this.swapsAxes = swapsAxes;
        this.flipsX = flipsX;
        this.flipsZ = flipsZ;
    }

    public static FoldOrientation of(boolean flipsX, boolean flipsZ) {
        return of(false, flipsX, flipsZ);
    }

    public static FoldOrientation of(boolean swapsAxes, boolean flipsX, boolean flipsZ) {
        return BY_PARTS[(swapsAxes ? SWAP_BIT : 0) | (flipsX ? FLIP_X_BIT : 0) | (flipsZ ? FLIP_Z_BIT : 0)];
    }

    public boolean swapsAxes() {
        return this.swapsAxes;
    }

    public boolean flipsX() {
        return this.flipsX;
    }

    public boolean flipsZ() {
        return this.flipsZ;
    }

    public boolean isIdentity() {
        return this == IDENTITY;
    }

    public boolean preservesHandedness() {
        return (this.flipsX == this.flipsZ) != this.swapsAxes;
    }

    public FoldOrientation compose(FoldOrientation next) {
        boolean carriedX = next.swapsAxes ? this.flipsZ : this.flipsX;
        boolean carriedZ = next.swapsAxes ? this.flipsX : this.flipsZ;
        return of(this.swapsAxes ^ next.swapsAxes, next.flipsX ^ carriedX, next.flipsZ ^ carriedZ);
    }

    public Vec3 applyToDelta(Vec3 delta) {
        if (isIdentity()) {
            return delta;
        }

        double sourceX = this.swapsAxes ? delta.z : delta.x;
        double sourceZ = this.swapsAxes ? delta.x : delta.z;
        return new Vec3(this.flipsX ? -sourceX : sourceX, delta.y, this.flipsZ ? -sourceZ : sourceZ);
    }

    public Vec3 applyToBlockOffset(Vec3 offset) {
        if (isIdentity()) {
            return offset;
        }

        double sourceX = this.swapsAxes ? offset.z : offset.x;
        double sourceZ = this.swapsAxes ? offset.x : offset.z;
        return new Vec3(
                this.flipsX ? BLOCK_WIDTH - sourceX : sourceX,
                offset.y,
                this.flipsZ ? BLOCK_WIDTH - sourceZ : sourceZ);
    }

    public Direction applyToFace(Direction face) {
        if (isIdentity() || face.getAxis() == Direction.Axis.Y) {
            return face;
        }

        return faceOf(face.getStepX(), face.getStepZ());
    }

    private Direction faceOf(int stepX, int stepZ) {
        int sourceX = this.swapsAxes ? stepZ : stepX;
        int sourceZ = this.swapsAxes ? stepX : stepZ;
        int turnedX = this.flipsX ? -sourceX : sourceX;
        int turnedZ = this.flipsZ ? -sourceZ : sourceZ;
        if (turnedX != 0) {
            return turnedX > 0 ? Direction.EAST : Direction.WEST;
        }

        return turnedZ > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
