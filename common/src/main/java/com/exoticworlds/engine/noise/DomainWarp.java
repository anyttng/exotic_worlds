package com.exoticworlds.engine.noise;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

public final class DomainWarp {
    public record Divisor(WorldFold fold, double value) {
    }

    public static double applyX(TranslationLattice lattice, int blockX, int blockZ, double shift, double divisor) {
        return lattice.foldX(blockX, blockZ) + shift / divisor;
    }

    public static double applyZ(TranslationLattice lattice, int blockZ, double shift, double divisor) {
        return lattice.foldZ(blockZ) + shift / divisor;
    }

    public static boolean carries(TranslationLattice lattice, int blockX, int blockZ, double shiftX, double shiftZ,
            double vanillaXzScale) {
        return lattice.x().canLap(applyX(lattice, blockX, blockZ, shiftX, vanillaXzScale))
                && lattice.z().canLap(applyZ(lattice, blockZ, shiftZ, vanillaXzScale));
    }

    private DomainWarp() {
    }
}
