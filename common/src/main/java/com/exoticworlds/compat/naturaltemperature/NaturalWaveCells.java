package com.exoticworlds.compat.naturaltemperature;

import com.exoticworlds.compat.naturaltemperature.mixin.MathUtilsAccessor;

public final class NaturalWaveCells implements WaveCells {
    public static final NaturalWaveCells INSTANCE = new NaturalWaveCells();

    private static final double ONE_CELL = 1.0;

    private NaturalWaveCells() {
    }

    @Override
    public double pointShare(long seed, long cell) {
        return MathUtilsAccessor.exotic_worlds$pointPosition(cell, ONE_CELL, seed) - cell;
    }

    @Override
    public double value(long seed, long cell) {
        return 2.0 * MathUtilsAccessor.exotic_worlds$random01(seed, cell, MathUtilsAccessor.exotic_worlds$valueSalt())
                - 1.0;
    }
}
