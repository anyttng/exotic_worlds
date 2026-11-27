package com.exoticworlds.compat.lithostitched;

import com.exoticworlds.core.world.Divisors;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;
import com.google.common.math.IntMath;

public record CellLap(TranslationLattice lattice, int widthBlocks) {
    private static final int NO_LAP = 0;

    public static CellLap of(WorldFold fold, int modWidthBlocks) {
        TranslationLattice lattice = fold.getBlockLattice();
        return new CellLap(lattice, widthBlocks(lattice, modWidthBlocks));
    }

    static int widthBlocks(TranslationLattice lattice, int modWidthBlocks) {
        int common = IntMath.gcd(lapOf(lattice.getX()), lapOf(lattice.getZ()));
        if (lattice.isSkewed()) {
            common = IntMath.gcd(common, Math.abs(lattice.getSkew()));
        }

        if (common == NO_LAP) {
            return modWidthBlocks;
        }

        int widest = 1;
        for (int divisor : Divisors.of(common)) {
            if (divisor <= modWidthBlocks) {
                widest = divisor;
            }
        }

        return widest;
    }

    public int foldX(int cellX, int cellZ) {
        return Math.floorDiv(this.lattice.foldX(cellX * this.widthBlocks, cellZ * this.widthBlocks), this.widthBlocks);
    }

    public int foldZ(int cellZ) {
        return Math.floorDiv(this.lattice.foldZ(cellZ * this.widthBlocks), this.widthBlocks);
    }

    private static int lapOf(WrapDomain domain) {
        return domain.isLooped() ? domain.domainLength : NO_LAP;
    }
}
