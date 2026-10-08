package com.exoticworlds.compat.reterraforged;

import net.minecraft.core.Direction;

// A river network is carved around its continent's canonical centre, each point seated on the copy nearest that
// centre, so the network has to fit inside half a lap of it or the seating cuts it at the far side of the world.
public final class RiverReach {
    // A root's forks reach 0.44 of their parent's length, two levels deep: 1 + 0.44 + 0.44^2, rounded up.
    private static final float NETWORK_REACH = 1.64F;

    // The widest main valley (275 x 1.5), the root warp (175) and wiggle (45), the carve warp (30) and the lake warp
    // (350), rounded up.
    private static final float CARVE_MARGIN = 1024.0F;

    // Twice the 400 blocks a root starts out from its centre, so a root still runs past its own start.
    private static final float MIN_ROOT_LENGTH = 800.0F;

    private static final float HALF = 0.5F;

    public static float clamp(RtfLap.Frame frame, float length, float dx, float dz) {
        float reach = Math.min(axisReach(frame, Direction.Axis.X, dx), axisReach(frame, Direction.Axis.Z, dz));
        return Math.min(length, reach);
    }

    public static boolean fits(RtfLap.Frame frame) {
        return axisReach(frame, Direction.Axis.X, 1.0F) >= MIN_ROOT_LENGTH
                && axisReach(frame, Direction.Axis.Z, 1.0F) >= MIN_ROOT_LENGTH;
    }

    private static float axisReach(RtfLap.Frame frame, Direction.Axis axis, float direction) {
        double lap = frame.lap(axis);
        if (lap == RtfLap.OPEN || direction == 0.0F) {
            return Float.MAX_VALUE;
        }

        return (float) ((lap * HALF - CARVE_MARGIN) / (Math.abs(direction) * NETWORK_REACH));
    }

    private RiverReach() {
    }
}
