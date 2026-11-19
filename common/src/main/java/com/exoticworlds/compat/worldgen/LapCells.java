package com.exoticworlds.compat.worldgen;

public final class LapCells {
    private static final double CELL_EPSILON = 1.0E-9;

    public static long whole(double cells, long floor) {
        return Math.max(floor, (long) Math.ceil(cells - CELL_EPSILON));
    }

    private LapCells() {
    }
}
