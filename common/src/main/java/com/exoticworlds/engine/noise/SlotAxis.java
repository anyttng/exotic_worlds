package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WrapDomain;

public enum SlotAxis {
    X,
    Z,
    NONE;

    private static final WrapDomain UNWRAPPED = new WrapDomain.Noop();

    public boolean hasWorldAxis() {
        return this != NONE;
    }

    // mc/26.2: the shift noise mixin scales an unwrapped slot through it.
    public double samplerInput(double coord, double uniformScale) {
        return hasWorldAxis() ? coord : coord * uniformScale;
    }

    public WrapDomain getDomain(TranslationLattice lattice) {
        return switch (this) {
            case X -> lattice.getX();
            case Z -> lattice.getZ();
            case NONE -> UNWRAPPED;
        };
    }

    public double getDivisor(NoiseFrame frame) {
        return switch (this) {
            case X -> frame.xDivisor();
            case Z -> frame.zDivisor();
            case NONE -> NoiseConstants.UNDIVIDED;
        };
    }
}
