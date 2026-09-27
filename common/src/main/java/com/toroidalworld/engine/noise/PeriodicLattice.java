package com.toroidalworld.engine.noise;

import com.toroidalworld.core.WrapDomain;

record PeriodicLattice(byte[] permutations, double xOffset, double yOffset, double zOffset, double fudgeYScale,
        Axis x, Axis y, Axis z, double correction, double anchor) {
    record Axis(boolean folds, WrapDomain domain, long period, double scale) {
        static final Axis PASS_THROUGH = new Axis(false, new WrapDomain.Noop(), PeriodicNoiseSampler.UNBOUNDED_PERIOD,
                1.0);

        double coord(double coord) {
            return this.folds ? PeriodicNoiseSampler.foldAndScale(this.domain, this.period, this.scale, coord) : coord;
        }
    }
}
