package com.exoticworlds.compat.worldgen;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;

public final class LapNoise {
    public static double eval(OpenSimplexStandIn noise, WorldFold fold, double x, double z, double xScale,
            double zScale) {
        TranslationLattice lattice = fold.getBlockLattice();
        return OpenSimplexQuantiles.matched(noise.eval(frameX(lattice, x, z, xScale, zScale), z,
                period(lattice.getX(), xScale), period(lattice.getZ(), zScale)));
    }

    public static double eval(OpenSimplexStandIn noise, WorldFold fold, double x, double y, double z, double xScale,
            double zScale) {
        TranslationLattice lattice = fold.getBlockLattice();
        return OpenSimplexQuantiles.matched3d(noise.eval(frameX(lattice, x, z, xScale, zScale), y, z,
                period(lattice.getX(), xScale), period(lattice.getZ(), zScale)));
    }

    static double period(WrapDomain domain, double scale) {
        return domain.isLooped() ? domain.domainLength * scale : OpenSimplexStandIn.UNBOUNDED;
    }

    private static double frameX(TranslationLattice lattice, double x, double z, double xScale, double zScale) {
        return lattice.isSkewed() ? xScale * lattice.rectangleX(x / xScale, z / zScale) : x;
    }

    private LapNoise() {
    }
}
