package com.exoticworlds.core;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public enum FoldOrientation {
    IDENTITY(false, false, false, Mirror.NONE, Rotation.NONE),
    MIRROR_X(false, true, false, Mirror.FRONT_BACK, Rotation.NONE),
    MIRROR_Z(false, false, true, Mirror.LEFT_RIGHT, Rotation.NONE),
    HALF_TURN(false, true, true, Mirror.NONE, Rotation.CLOCKWISE_180),
    QUARTER_TURN_CLOCKWISE(true, true, false, Mirror.NONE, Rotation.CLOCKWISE_90),
    QUARTER_TURN_COUNTERCLOCKWISE(true, false, true, Mirror.NONE, Rotation.COUNTERCLOCKWISE_90),
    MIRROR_DIAGONAL(true, false, false, Mirror.FRONT_BACK, Rotation.COUNTERCLOCKWISE_90),
    MIRROR_ANTIDIAGONAL(true, true, true, Mirror.FRONT_BACK, Rotation.CLOCKWISE_90);

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
    private final Mirror stateMirror;
    private final Rotation stateRotation;

    FoldOrientation(boolean swapsAxes, boolean flipsX, boolean flipsZ, Mirror stateMirror, Rotation stateRotation) {
        this.swapsAxes = swapsAxes;
        this.flipsX = flipsX;
        this.flipsZ = flipsZ;
        this.stateMirror = stateMirror;
        this.stateRotation = stateRotation;
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

    public int applyToLocalX(int localX, int localZ, int width) {
        return localOf(this.flipsX, this.swapsAxes ? localZ : localX, width);
    }

    public int applyToLocalZ(int localX, int localZ, int width) {
        return localOf(this.flipsZ, this.swapsAxes ? localX : localZ, width);
    }

    public int[] localIndexTable(int width) {
        int[] table = new int[width * width];
        for (int localZ = 0; localZ < width; localZ++) {
            for (int localX = 0; localX < width; localX++) {
                table[localZ * width + localX] =
                        applyToLocalZ(localX, localZ, width) * width + applyToLocalX(localX, localZ, width);
            }
        }

        return table;
    }

    public Mirror stateMirror() {
        return this.stateMirror;
    }

    public Rotation stateRotation() {
        return this.stateRotation;
    }

    public BlockState applyToState(BlockState state) {
        if (isIdentity()) {
            return state;
        }

        return state.mirror(this.stateMirror).rotate(this.stateRotation);
    }

    private static int localOf(boolean flips, int source, int width) {
        return flips ? width - 1 - source : source;
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
