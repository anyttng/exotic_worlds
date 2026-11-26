package com.exoticworlds.core.fold;

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

    private static final float QUARTER_TURN_DEGREES = 90.0F;
    private static final float HALF_TURN_DEGREES = 180.0F;

    private final boolean isSwappingAxes;
    private final boolean isXFlipped;
    private final boolean isZFlipped;
    private final Mirror stateMirror;
    private final Rotation stateRotation;
    private final float yawOffsetDegrees;

    FoldOrientation(boolean isSwappingAxes, boolean isXFlipped, boolean isZFlipped, Mirror stateMirror, Rotation stateRotation) {
        this.isSwappingAxes = isSwappingAxes;
        this.isXFlipped = isXFlipped;
        this.isZFlipped = isZFlipped;
        this.stateMirror = stateMirror;
        this.stateRotation = stateRotation;
        this.yawOffsetDegrees = yawOffsetDegreesOf(stateMirror) + yawOffsetDegreesOf(stateRotation);
    }

    private static float yawOffsetDegreesOf(Mirror mirror) {
        return mirror == Mirror.LEFT_RIGHT ? HALF_TURN_DEGREES : 0.0F;
    }

    private static float yawOffsetDegreesOf(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0.0F;
            case CLOCKWISE_90 -> QUARTER_TURN_DEGREES;
            case CLOCKWISE_180 -> HALF_TURN_DEGREES;
            case COUNTERCLOCKWISE_90 -> -QUARTER_TURN_DEGREES;
        };
    }

    public static FoldOrientation of(boolean isSwappingAxes, boolean isXFlipped, boolean isZFlipped) {
        return BY_PARTS[(isSwappingAxes ? SWAP_BIT : 0) | (isXFlipped ? FLIP_X_BIT : 0) | (isZFlipped ? FLIP_Z_BIT : 0)];
    }

    public boolean isSwappingAxes() {
        return this.isSwappingAxes;
    }

    public boolean isXFlipped() {
        return this.isXFlipped;
    }

    public boolean isZFlipped() {
        return this.isZFlipped;
    }

    public boolean isIdentity() {
        return this == IDENTITY;
    }

    public boolean isHandednessPreserved() {
        return (this.isXFlipped == this.isZFlipped) != this.isSwappingAxes;
    }

    public FoldOrientation compose(FoldOrientation next) {
        boolean isXCarried = next.isSwappingAxes ? this.isZFlipped : this.isXFlipped;
        boolean isZCarried = next.isSwappingAxes ? this.isXFlipped : this.isZFlipped;
        return of(this.isSwappingAxes ^ next.isSwappingAxes, next.isXFlipped ^ isXCarried, next.isZFlipped ^ isZCarried);
    }

    public FoldOrientation inverse() {
        return this.isSwappingAxes ? of(true, this.isZFlipped, this.isXFlipped) : this;
    }

    public Vec3 applyToDelta(Vec3 delta) {
        if (isIdentity()) {
            return delta;
        }

        double sourceX = this.isSwappingAxes ? delta.z : delta.x;
        double sourceZ = this.isSwappingAxes ? delta.x : delta.z;
        return new Vec3(this.isXFlipped ? -sourceX : sourceX, delta.y, this.isZFlipped ? -sourceZ : sourceZ);
    }

    public float applyToYaw(float yaw) {
        if (isIdentity()) {
            return yaw;
        }

        return applyToYawChange(yaw) + this.yawOffsetDegrees;
    }

    public float applyToYawChange(float change) {
        return isHandednessPreserved() ? change : -change;
    }

    public Vec3 applyToBlockOffset(Vec3 offset) {
        if (isIdentity()) {
            return offset;
        }

        double sourceX = this.isSwappingAxes ? offset.z : offset.x;
        double sourceZ = this.isSwappingAxes ? offset.x : offset.z;
        return new Vec3(
                this.isXFlipped ? BLOCK_WIDTH - sourceX : sourceX,
                offset.y,
                this.isZFlipped ? BLOCK_WIDTH - sourceZ : sourceZ);
    }

    public Direction applyToFace(Direction face) {
        if (isIdentity() || face.getAxis() == Direction.Axis.Y) {
            return face;
        }

        return faceOf(face.getStepX(), face.getStepZ());
    }

    public int applyToLocalX(int localX, int localZ, int width) {
        return localOf(this.isXFlipped, this.isSwappingAxes ? localZ : localX, width);
    }

    public int applyToLocalZ(int localX, int localZ, int width) {
        return localOf(this.isZFlipped, this.isSwappingAxes ? localX : localZ, width);
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

    public Mirror getStateMirror() {
        return this.stateMirror;
    }

    public Rotation getStateRotation() {
        return this.stateRotation;
    }

    public BlockState applyToState(BlockState state) {
        if (isIdentity()) {
            return state;
        }

        return state.mirror(this.stateMirror).rotate(this.stateRotation);
    }

    private static int localOf(boolean isFlipped, int source, int width) {
        return isFlipped ? width - 1 - source : source;
    }

    private Direction faceOf(int stepX, int stepZ) {
        int sourceX = this.isSwappingAxes ? stepZ : stepX;
        int sourceZ = this.isSwappingAxes ? stepX : stepZ;
        int turnedX = this.isXFlipped ? -sourceX : sourceX;
        int turnedZ = this.isZFlipped ? -sourceZ : sourceZ;
        if (turnedX != 0) {
            return turnedX > 0 ? Direction.EAST : Direction.WEST;
        }

        return turnedZ > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
