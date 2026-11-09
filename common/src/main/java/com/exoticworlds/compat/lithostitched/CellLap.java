package com.exoticworlds.compat.lithostitched;

import com.exoticworlds.core.Divisors;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.google.common.math.IntMath;

public record CellLap(TranslationLattice lattice, int width) {
    private static final int NO_LAP = 0;

    public static CellLap of(WorldFold fold, int modWidth) {
        TranslationLattice lattice = fold.blockLattice();
        return new CellLap(lattice, width(lattice, modWidth));
    }

    static int width(TranslationLattice lattice, int modWidth) {
        int common = IntMath.gcd(lapOf(lattice.x()), lapOf(lattice.z()));
        if (lattice.isSkewed()) {
            common = IntMath.gcd(common, Math.abs(lattice.skew()));
        }

        if (common == NO_LAP) {
            return modWidth;
        }

        int widest = 1;
        for (int divisor : Divisors.of(common)) {
            if (divisor <= modWidth) {
                widest = divisor;
            }
        }

        return widest;
    }

    public int foldX(int cellX, int cellZ) {
        return Math.floorDiv(this.lattice.foldX(cellX * this.width, cellZ * this.width), this.width);
    }

    public int foldZ(int cellZ) {
        return Math.floorDiv(this.lattice.foldZ(cellZ * this.width), this.width);
    }

    private static int lapOf(WrapDomain domain) {
        return domain.loops() ? domain.domainLength : NO_LAP;
    }
}
