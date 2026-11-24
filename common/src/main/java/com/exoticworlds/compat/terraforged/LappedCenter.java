package com.exoticworlds.compat.terraforged;

import net.minecraft.core.Direction;

public record LappedCenter<C>(double xLap, double zLap, double skew, C center) {
    public static <C> LappedCenter<C> on(RtfLap.Frame frame, C center) {
        return new LappedCenter<>(frame.lap(Direction.Axis.X), frame.lap(Direction.Axis.Z), frame.getSkew(), center);
    }

    public boolean isValidFor(RtfLap.Frame frame) {
        return this.xLap == frame.lap(Direction.Axis.X) && this.zLap == frame.lap(Direction.Axis.Z)
                && this.skew == frame.getSkew();
    }
}
