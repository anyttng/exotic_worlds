package com.exoticworlds.compat.wover;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;

public final class LapNoise {
    public static double eval(OpenSimplexStandIn noise, WorldFold fold, double x, double z, double xScale,
            double zScale) {
        return OpenSimplexQuantiles.matched(
                noise.eval(x, z, period(fold, Direction.Axis.X, xScale), period(fold, Direction.Axis.Z, zScale)));
    }

    public static double eval(OpenSimplexStandIn noise, WorldFold fold, double x, double y, double z, double xScale,
            double zScale) {
        return OpenSimplexQuantiles.matched3d(
                noise.eval(x, y, z, period(fold, Direction.Axis.X, xScale), period(fold, Direction.Axis.Z, zScale)));
    }

    static double period(WorldFold fold, Direction.Axis axis, double scale) {
        WrapDomain domain = fold.blockDomain(axis);
        return domain.loops() ? domain.domainLength * scale : OpenSimplexStandIn.UNBOUNDED;
    }

    private LapNoise() {
    }
}
