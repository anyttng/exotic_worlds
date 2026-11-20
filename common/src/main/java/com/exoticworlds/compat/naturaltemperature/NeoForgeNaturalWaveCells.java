package com.exoticworlds.compat.naturaltemperature;

import com.exoticworlds.compat.naturaltemperature.mixin.NeoForgeMathUtilsAccessor;

public final class NeoForgeNaturalWaveCells implements WaveCells {
    public static final NeoForgeNaturalWaveCells INSTANCE = new NeoForgeNaturalWaveCells();

    private static final double ONE_CELL = 1.0;

    private NeoForgeNaturalWaveCells() {
    }

    @Override
    public double pointShare(long seed, long cell) {
        return NeoForgeMathUtilsAccessor.exotic_worlds$pointPosition(cell, ONE_CELL, seed) - cell;
    }

    @Override
    public double value(long seed, long cell) {
        return 2.0 * NeoForgeMathUtilsAccessor.exotic_worlds$random01(seed, cell, NeoForgeMathUtilsAccessor.exotic_worlds$valueSalt())
                - 1.0;
    }
}
