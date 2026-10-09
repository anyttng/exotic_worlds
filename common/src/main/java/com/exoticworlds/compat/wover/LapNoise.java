package com.exoticworlds.compat.wover;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

public final class LapNoise {
    public static double eval(OpenSimplexStandIn noise, WorldFold fold, double x, double z, double xScale,
            double zScale) {
        TranslationLattice lattice = fold.blockLattice();
        return OpenSimplexQuantiles.matched(noise.eval(frameX(lattice, x, z, xScale, zScale), z,
                period(lattice.x(), xScale), period(lattice.z(), zScale)));
    }

    public static double eval(OpenSimplexStandIn noise, WorldFold fold, double x, double y, double z, double xScale,
            double zScale) {
        TranslationLattice lattice = fold.blockLattice();
        return OpenSimplexQuantiles.matched3d(noise.eval(frameX(lattice, x, z, xScale, zScale), y, z,
                period(lattice.x(), xScale), period(lattice.z(), zScale)));
    }

    static double period(WrapDomain domain, double scale) {
        return domain.loops() ? domain.domainLength * scale : OpenSimplexStandIn.UNBOUNDED;
    }

    private static double frameX(TranslationLattice lattice, double x, double z, double xScale, double zScale) {
        return lattice.isSkewed() ? xScale * lattice.rectangleX(x / xScale, z / zScale) : x;
    }

    private LapNoise() {
    }
}
