package com.exoticworlds.engine.net;

import java.util.Arrays;

import com.exoticworlds.core.fold.FoldOrientation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

final class EventDirections {
    private static final int DIRECTIONS = Direction.values().length;

    private static final int PISTON_DIRECTION_MASK = 7;

    private static final int BELL_RING_EVENT = 1;

    private static final int FACE_SET_MASK = (1 << DIRECTIONS) - 1;

    private static final int AXIS_X = Arrays.asList(Direction.Axis.VALUES).indexOf(Direction.Axis.X);

    private static final int AXIS_Z = Arrays.asList(Direction.Axis.VALUES).indexOf(Direction.Axis.Z);

    static int turnedBlockEventData(Block block, int event, int data, FoldOrientation orientation) {
        if (orientation.isIdentity()) {
            return data;
        }

        if (block instanceof PistonBaseBlock) {
            return (data & ~PISTON_DIRECTION_MASK) | turnedDirection(data & PISTON_DIRECTION_MASK, orientation);
        }

        return block instanceof BellBlock && event == BELL_RING_EVENT ? turnedDirection(data, orientation) : data;
    }

    static int turnedLevelEventData(int type, int data, FoldOrientation orientation) {
        if (orientation.isIdentity()) {
            return data;
        }

        return switch (type) {
            case LevelEvent.PARTICLES_SHOOT_SMOKE, LevelEvent.PARTICLES_SHOOT_WHITE_SMOKE ->
                    turnedDirection(data, orientation);
            case LevelEvent.PARTICLES_ELECTRIC_SPARK -> turnedAxis(data, orientation);
            case LevelEvent.PARTICLES_SCULK_CHARGE ->
                    (data & ~FACE_SET_MASK) | turnedFaceSet(data & FACE_SET_MASK, orientation);
            default -> data;
        };
    }

    private static int turnedDirection(int data, FoldOrientation orientation) {
        if (data < 0 || data >= DIRECTIONS) {
            return data;
        }

        return orientation.applyToFace(Direction.from3DDataValue(data)).get3DDataValue();
    }

    private static int turnedAxis(int data, FoldOrientation orientation) {
        if (!orientation.isSwappingAxes()) {
            return data;
        }

        if (data == AXIS_X) {
            return AXIS_Z;
        }

        return data == AXIS_Z ? AXIS_X : data;
    }

    private static int turnedFaceSet(int faces, FoldOrientation orientation) {
        int turned = 0;
        for (Direction face : Direction.values()) {
            if ((faces & (1 << face.get3DDataValue())) != 0) {
                turned |= 1 << orientation.applyToFace(face).get3DDataValue();
            }
        }

        return turned;
    }

    private EventDirections() {
    }
}
