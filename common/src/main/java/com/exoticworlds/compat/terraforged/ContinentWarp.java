package com.exoticworlds.compat.terraforged;

import net.minecraft.core.Direction;

public final class ContinentWarp {
    private static final double UNIT = 1.0;

    public static float ratio(RtfLap.Frame frame, Direction.Axis axis, float frequency) {
        return (float) frame.cellRatio(axis, frequency);
    }

    public static RtfLap.Frame.Scope enter(RtfLap.Frame frame, float xRatio, float zRatio) {
        return frame.scale(UNIT / xRatio, UNIT / zRatio);
    }

    private ContinentWarp() {
    }
}
