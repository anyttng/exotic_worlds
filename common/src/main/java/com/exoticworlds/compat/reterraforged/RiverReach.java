package com.exoticworlds.compat.reterraforged;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.cell.continent.SimpleContinent;

public final class RiverReach {
    // A root's forks reach 0.44 of their parent's length, two levels deep: 1 + 0.44 + 0.44^2, rounded up.
    private static final float NETWORK_REACH = 1.64F;

    // The widest main valley (275 x 1.5), the root warp (175) and wiggle (45), the carve warp (30) and the lake warp
    // (350), rounded up.
    private static final float CARVE_MARGIN = 1024.0F;

    // Twice the 400 blocks a root starts out from its centre, so a root still runs past its own start.
    private static final float MIN_ROOT_LENGTH = 800.0F;

    private static final float HALF = 0.5F;

    private static final float COAST_STEP = 8.0F;

    private static final float COAST_PRECISION = 10.0F;

    public static final float UNBOUNDED = Float.MAX_VALUE;

    private static final int CONTINENT_CELL_PER_SCALE = 4;

    private static final double WHOLE_CELL = 1.0;

    private static final int MIN_ROOTS = 1;

    public static float clamp(RtfLap.Frame frame, float length, float dx, float dz) {
        return Math.min(length, reach(frame, dx, dz));
    }

    public static float reach(RtfLap.Frame frame, float dx, float dz) {
        return Math.min(axisReach(frame, Direction.Axis.X, dx), axisReach(frame, Direction.Axis.Z, dz));
    }

    public static float distanceToOcean(SimpleContinent continent, float shallowOcean, int x, int z, float dx,
            float dz, float reach) {
        float land = 0.0F;
        for (float sea = COAST_STEP; sea < reach; sea += COAST_STEP) {
            if (continent.getEdgeValue(x + dx * sea, z + dz * sea) <= shallowOcean) {
                return coastBetween(continent, shallowOcean, x, z, dx, dz, land, sea);
            }

            land = sea;
        }

        return reach;
    }

    private static float coastBetween(SimpleContinent continent, float shallowOcean, int x, int z, float dx, float dz,
            float land, float sea) {
        while (sea - land >= COAST_PRECISION) {
            float mid = (land + sea) * HALF;
            if (continent.getEdgeValue(x + dx * mid, z + dz * mid) > shallowOcean) {
                land = mid;
            } else {
                sea = mid;
            }
        }

        return sea;
    }

    public static int rootCount(RtfLap.Frame frame, int count, int continentScale) {
        double frequency = 1.0 / ((double) continentScale * CONTINENT_CELL_PER_SCALE);
        double xRatio = cellRatio(frame, Direction.Axis.X, frequency);
        double zRatio = cellRatio(frame, Direction.Axis.Z, frequency);
        return Math.max(MIN_ROOTS, (int) Math.round(count * Math.sqrt(xRatio * zRatio)));
    }

    private static double cellRatio(RtfLap.Frame frame, Direction.Axis axis, double frequency) {
        double lap = frame.lap(axis);
        if (lap == RtfLap.OPEN) {
            return WHOLE_CELL;
        }

        return lap * frequency / frame.cells(axis, frequency);
    }

    public static boolean fits(RtfLap.Frame frame) {
        return axisReach(frame, Direction.Axis.X, 1.0F) >= MIN_ROOT_LENGTH
                && axisReach(frame, Direction.Axis.Z, 1.0F) >= MIN_ROOT_LENGTH;
    }

    private static float axisReach(RtfLap.Frame frame, Direction.Axis axis, float direction) {
        double lap = frame.lap(axis);
        if (lap == RtfLap.OPEN || direction == 0.0F) {
            return UNBOUNDED;
        }

        return (float) ((lap * HALF - CARVE_MARGIN) / (Math.abs(direction) * NETWORK_REACH));
    }

    private RiverReach() {
    }
}
