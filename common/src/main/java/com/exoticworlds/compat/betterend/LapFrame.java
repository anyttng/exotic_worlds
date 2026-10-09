package com.exoticworlds.compat.betterend;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WrapDomain;

public final class LapFrame {
    private static final double UNSHEARED = 0.0;

    private final TranslationLattice lattice;

    private final double shear;

    public LapFrame(TranslationLattice lattice) {
        this.lattice = lattice;
        this.shear = lattice.isSkewed() ? (double) lattice.skew() / lattice.z().domainLength : UNSHEARED;
    }

    public double frameX(double blockX, double blockZ) {
        return blockX - blockZ * this.shear;
    }

    public double blockX(double frameX, double blockZ) {
        return frameX + blockZ * this.shear;
    }

    public double shear() {
        return this.shear;
    }

    public long shiftX(long lapsX, long lapsZ) {
        return lapsX * lapLength(this.lattice.x()) + lapsZ * this.lattice.skew();
    }

    public long shiftZ(long lapsZ) {
        return lapsZ * lapLength(this.lattice.z());
    }

    public long[] nearestOriginLaps(double blockX, double blockZ, long pickX, long pickZ) {
        if (!this.lattice.isSkewed()) {
            return new long[] {pickX, pickZ};
        }

        long bestX = pickX;
        long bestZ = pickZ;
        double best = squaredFromCopy(blockX, blockZ, pickX, pickZ);
        long[] shifts = this.lattice.nearShifts(blockX, blockZ);
        for (int index = 0; index < shifts.length; index += 2) {
            long lapsZ = lapsOf(shifts[index + 1], this.lattice.z());
            long lapsX = lapsOf(shifts[index] - lapsZ * this.lattice.skew(), this.lattice.x());
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
        return domain.loops() ? shift / domain.domainLength : 0L;
    }

    private static long lapLength(WrapDomain domain) {
        return domain.loops() ? domain.domainLength : 0L;
    }
}
