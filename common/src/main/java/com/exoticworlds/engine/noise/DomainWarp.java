package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;

public final class DomainWarp {
    public record Divisor(WorldFold fold, double value) {
    }

    public static double applyX(TranslationLattice lattice, int blockX, int blockZ, double shiftBlocks, double divisor) {
        return lattice.foldX(blockX, blockZ) + shiftBlocks / divisor;
    }

    public static double applyZ(TranslationLattice lattice, int blockZ, double shiftBlocks, double divisor) {
        return lattice.foldZ(blockZ) + shiftBlocks / divisor;
    }

    public static boolean canCarry(TranslationLattice lattice, int blockX, int blockZ, double shiftX, double shiftZ,
            double vanillaXzScale) {
        return lattice.getX().canLap(applyX(lattice, blockX, blockZ, shiftX, vanillaXzScale))
                && lattice.getZ().canLap(applyZ(lattice, blockZ, shiftZ, vanillaXzScale));
    }

    private DomainWarp() {
    }
}
