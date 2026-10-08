package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WrapDomain;

record PeriodicLattice(byte[] permutations, double xOffset, double yOffset, double zOffset,
        Axis x, Axis y, Axis z, double correction, double anchor, @Nullable Shear shear) {
    record Axis(boolean folds, WrapDomain domain, long period, double scale) {
        static final Axis PASS_THROUGH = new Axis(false, new WrapDomain.Noop(), PeriodicNoiseSampler.UNBOUNDED_PERIOD,
                1.0);

        double coord(double coord) {
            return this.folds ? PeriodicNoiseSampler.foldAndScale(this.domain, this.period, this.scale, coord) : coord;
        }
    }

    record Shear(TranslationLattice lattice, int xSlot, int zSlot, long xPeriod, long zPeriod, long skewCells,
            double xRatio, double zRatio, double xPerZ) {
        static Shear of(TranslationLattice lattice, int xSlot, int zSlot, long xPeriod, long zPeriod) {
            double xRatio = (double) xPeriod / lattice.x().domainLength;
            double exactSkewCells = xRatio * lattice.skew();
            long skewCells = Math.round(exactSkewCells);
            double xPerZ = (skewCells - exactSkewCells) / lattice.z().domainLength;
            return new Shear(lattice, xSlot, zSlot, xPeriod, zPeriod, skewCells, xRatio,
                    (double) zPeriod / lattice.z().domainLength, xPerZ);
        }

        double noiseX(double worldX, double worldZ) {
            double foldedZ = this.lattice.foldZ(worldZ);
            return this.xRatio * this.lattice.foldX(worldX, worldZ) + this.xPerZ * foldedZ;
        }

        double noiseZ(double worldZ) {
            return this.zRatio * this.lattice.foldZ(worldZ);
        }
    }
}
