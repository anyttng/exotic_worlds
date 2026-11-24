package com.exoticworlds.engine.noise.periodic;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WrapDomain;

@SuppressWarnings("ArrayRecordComponent")
public record PeriodicLattice(byte[] permutations, double xOffset, double yOffset, double zOffset, double fudgeYScale,
        Axis x, Axis y, Axis z, double correction, double anchor, @Nullable Shear shear) {
    record Axis(boolean isFolded, WrapDomain domain, long period, double scale, double ratio) {
        static final Axis PASS_THROUGH = of(false, new WrapDomain.Noop(), PeriodicNoiseSampler.UNBOUNDED_PERIOD, 1.0);

        static Axis of(boolean isFolded, WrapDomain domain, long period, double scale) {
            return new Axis(isFolded, domain, period, scale, PeriodicNoiseSampler.ratio(domain, period));
        }

        double coord(double coord) {
            return this.isFolded
                    ? PeriodicNoiseSampler.foldAndScale(this.domain, this.period, this.scale, this.ratio, coord)
                    : coord;
        }

        boolean isIndexable() {
            return this.period <= PeriodicNoiseSampler.MAX_INDEXED_PERIOD;
        }
    }

    boolean isIndexable() {
        return this.x.isIndexable() && this.y.isIndexable() && this.z.isIndexable();
    }

    record Shear(TranslationLattice lattice, int xSlot, int zSlot, long xPeriod, long zPeriod, long skewCells,
            double xRatio, double zRatio, double xPerZ) {
        static Shear of(TranslationLattice lattice, int xSlot, int zSlot, long xPeriod, long zPeriod) {
            double xRatio = (double) xPeriod / lattice.getX().domainLength;
            double exactSkewCells = xRatio * lattice.getSkew();
            long skewCells = Math.round(exactSkewCells);
            double xPerZ = (skewCells - exactSkewCells) / lattice.getZ().domainLength;
            return new Shear(lattice, xSlot, zSlot, xPeriod, zPeriod, skewCells, xRatio,
                    (double) zPeriod / lattice.getZ().domainLength, xPerZ);
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
