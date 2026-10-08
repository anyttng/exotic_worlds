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

    private DomainWarp() {
    }
}
