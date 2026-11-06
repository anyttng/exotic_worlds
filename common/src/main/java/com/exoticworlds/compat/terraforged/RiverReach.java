package com.exoticworlds.compat.terraforged;

import net.minecraft.core.Direction;

public final class RiverReach {
    private static final float ROOT_START = 400.0F;

    private static final float RUN_PER_START = 2.0F;

    private static final float FULL_SIZE = 1.0F;

    private static final float HALF = 0.5F;

    private static final float COAST_STEP = 8.0F;

    public static final float UNBOUNDED = Float.MAX_VALUE;

    private static final int CONTINENT_CELL_PER_SCALE = 4;

    private static final int MIN_ROOTS = 1;

    private final float carveMargin;

    private final float fullSizeHalfLap;

    public RiverReach(float carveMargin) {
        this.carveMargin = carveMargin;
        this.fullSizeHalfLap = carveMargin + ROOT_START * RUN_PER_START;
    }

    public float clamp(RtfLap.Frame frame, float length) {
        return Math.min(length, reach(frame));
    }

    public float reach(RtfLap.Frame frame) {
        float margin = this.carveMargin * scale(frame);
        return Math.min(axisReach(frame, Direction.Axis.X, margin), axisReach(frame, Direction.Axis.Z, margin));
    }

    public float scale(RtfLap.Frame frame) {
        return Math.min(FULL_SIZE, Math.min(axisScale(frame, Direction.Axis.X), axisScale(frame, Direction.Axis.Z)));
    }

    public float boundScale() {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? FULL_SIZE : scale(frame);
    }

    private float axisScale(RtfLap.Frame frame, Direction.Axis axis) {
        double lap = frame.lap(axis);
        if (lap == RtfLap.OPEN) {
            return FULL_SIZE;
        }

        return (float) (lap * HALF / this.fullSizeHalfLap);
    }

    @FunctionalInterface
    public interface EdgeField {
        float at(float x, float z);
    }

    public static float distanceToOcean(EdgeField continent, float shallowOcean, int x, int z, float dx, float dz,
            float reach) {
        float land = 0.0F;
        float landEdge = continent.at(x, z);
        for (float sea = COAST_STEP; sea < reach; sea += COAST_STEP) {
            float seaEdge = continent.at(x + dx * sea, z + dz * sea);
            if (seaEdge <= shallowOcean) {
                return coastBetween(shallowOcean, land, landEdge, sea, seaEdge);
            }

            land = sea;
            landEdge = seaEdge;
        }

        return reach;
    }

    private static float coastBetween(float shallowOcean, float land, float landEdge, float sea, float seaEdge) {
        if (landEdge <= shallowOcean) {
            return sea;
        }

        return land + (sea - land) * (landEdge - shallowOcean) / (landEdge - seaEdge);
    }

    public static int rootCount(RtfLap.Frame frame, int count, int continentScale) {
        double frequency = 1.0 / ((double) continentScale * CONTINENT_CELL_PER_SCALE);
        double xRatio = frame.cellRatio(Direction.Axis.X, frequency);
        double zRatio = frame.cellRatio(Direction.Axis.Z, frequency);
        return Math.max(MIN_ROOTS, (int) Math.round(count * Math.sqrt(xRatio * zRatio)));
    }

    private static float axisReach(RtfLap.Frame frame, Direction.Axis axis, float margin) {
        double lap = frame.lap(axis);
        if (lap == RtfLap.OPEN) {
            return UNBOUNDED;
        }

        return (float) (lap * HALF - margin);
    }
}
