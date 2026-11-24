package com.exoticworlds.compat.worldgen;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WrapDomain;

public final class LapFrame {
    private static final double UNSHEARED = 0.0;

    private final TranslationLattice lattice;

    private final double shear;

    public LapFrame(TranslationLattice lattice) {
        this.lattice = lattice;
        this.shear = lattice.isSkewed() ? (double) lattice.getSkew() / lattice.getZ().domainLength : UNSHEARED;
    }

    public double frameX(double blockX, double blockZ) {
        return blockX - blockZ * this.shear;
    }

    public double blockX(double frameX, double blockZ) {
        return frameX + blockZ * this.shear;
    }

    public double getShear() {
        return this.shear;
    }

    public long shiftX(long lapsX, long lapsZ) {
        return lapsX * lapLength(this.lattice.getX()) + lapsZ * this.lattice.getSkew();
    }

    public long shiftZ(long lapsZ) {
        return lapsZ * lapLength(this.lattice.getZ());
    }

    public long[] nearestOriginLaps(double blockX, double blockZ, long pickX, long pickZ) {
        if (!this.lattice.isSkewed()) {
            return new long[] {pickX, pickZ};
        }

        long bestX = pickX;
        long bestZ = pickZ;
        double best = squaredFromCopy(blockX, blockZ, pickX, pickZ);
        long first = this.lattice.nearFirst(blockX, blockZ);
        long second = this.lattice.nearSecond(blockX, blockZ);
        for (int index = 0; index < this.lattice.getNearCount(); index++) {
            long lapsZ = lapsOf(this.lattice.nearShiftZ(first, second, index), this.lattice.getZ());
            long lapsX = lapsOf(this.lattice.nearShiftX(first, second, index) - lapsZ * this.lattice.getSkew(),
                    this.lattice.getX());
            double squared = squaredFromCopy(blockX, blockZ, lapsX, lapsZ);
            if (squared < best) {
                best = squared;
                bestX = lapsX;
                bestZ = lapsZ;
            }
        }

        return new long[] {bestX, bestZ};
    }

    private double squaredFromCopy(double blockX, double blockZ, long lapsX, long lapsZ) {
        double x = blockX - shiftX(lapsX, lapsZ);
        double z = blockZ - shiftZ(lapsZ);
        return x * x + z * z;
    }

    private static long lapsOf(long shift, WrapDomain domain) {
        return domain.isLooped() ? shift / domain.domainLength : 0L;
    }

    private static long lapLength(WrapDomain domain) {
        return domain.isLooped() ? domain.domainLength : 0L;
    }
}
