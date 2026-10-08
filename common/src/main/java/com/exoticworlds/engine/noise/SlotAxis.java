package com.exoticworlds.engine.noise;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WrapDomain;

public enum SlotAxis {
    X,
    Z,
    NONE;

    private static final WrapDomain UNWRAPPED = new WrapDomain.Noop();

    public boolean carriesWorldAxis() {
        return this != NONE;
    }

    public double samplerInput(double coord, double uniformScale) {
        return carriesWorldAxis() ? coord : coord * uniformScale;
    }

    public WrapDomain domainOf(TranslationLattice lattice) {
        return switch (this) {
            case X -> lattice.x();
            case Z -> lattice.z();
            case NONE -> UNWRAPPED;
        };
    }

    public double divisorIn(NoiseFrame frame) {
        return switch (this) {
            case X -> frame.xDivisor();
            case Z -> frame.zDivisor();
            case NONE -> NoiseConstants.UNDIVIDED;
        };
    }
}
