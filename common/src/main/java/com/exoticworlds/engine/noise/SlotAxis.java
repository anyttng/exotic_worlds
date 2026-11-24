package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;

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

    public double getDivisor(Context context) {
        return switch (this) {
            case X -> context.getXDivisor();
            case Z -> context.getZDivisor();
            case NONE -> NoiseConstants.UNDIVIDED;
        };
    }
}
