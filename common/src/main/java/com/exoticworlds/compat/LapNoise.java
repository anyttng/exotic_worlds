package com.exoticworlds.compat;

import java.util.function.DoubleSupplier;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.GenerationFoldContext;

public final class LapNoise {
    public static double boundEval(OpenSimplexStandIn noise, double x, double z, double xScale, double zScale,
            DoubleSupplier original) {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        return fold == null ? original.getAsDouble() : eval(noise, fold, x, z, xScale, zScale);
    }

    public static double boundEval(OpenSimplexStandIn noise, double x, double y, double z, double xScale,
            double zScale, DoubleSupplier original) {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        return fold == null ? original.getAsDouble() : eval(noise, fold, x, y, z, xScale, zScale);
    }

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
