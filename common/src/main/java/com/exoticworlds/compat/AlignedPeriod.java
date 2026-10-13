package com.exoticworlds.compat;

import com.exoticworlds.api.v1.ToroidalShape;

import net.minecraft.core.Direction;

public record AlignedPeriod(long xBlocks, long zBlocks, long xShiftBlocks) {
    private static final long NO_PERIOD = 0L;
    private static final int INVERSE_NEWTON_STEPS = 5;

    public static AlignedPeriod of(ToroidalShape shape, int skewBlocks, int alignExponent) {
        long x = shape.loops(Direction.Axis.X) ? blocks(shape.widthBlocks(Direction.Axis.X), alignExponent) : NO_PERIOD;
        if (!shape.loops(Direction.Axis.Z)) {
            return new AlignedPeriod(x, NO_PERIOD, 0L);
        }

        int widthZ = shape.widthBlocks(Direction.Axis.Z);
        int lapsExponent = alignExponent - Math.min(alignExponent, Integer.numberOfTrailingZeros(widthZ));
        if (skewBlocks == 0) {
            return new AlignedPeriod(x, ((long) widthZ) << lapsExponent, 0L);
        }

        int widthX = shape.widthBlocks(Direction.Axis.X);
        int alignX = Math.min(alignExponent, Integer.numberOfTrailingZeros(widthX));
        lapsExponent = Math.max(lapsExponent, alignX - Integer.numberOfTrailingZeros(skewBlocks));
        long rowShift = ((long) skewBlocks) << lapsExponent;
        return new AlignedPeriod(x, ((long) widthZ) << lapsExponent, shift(widthX, alignExponent, rowShift, x));
    }

    public static long blocks(int worldWidth, int alignExponent) {
        int gcd = 1 << Math.min(Integer.numberOfTrailingZeros(worldWidth), alignExponent);
        return (long) (worldWidth / gcd) << alignExponent;
    }

    private static long shift(int widthX, int alignExponent, long rowShift, long periodX) {
        int alignX = Integer.numberOfTrailingZeros(widthX);
        if (alignX >= alignExponent) {
            return Math.floorMod(rowShift, periodX);
        }

        long oddWidth = widthX >> alignX;
        long mask = (1L << (alignExponent - alignX)) - 1;
        long laps = (-(rowShift >> alignX) * inverseOfOdd(oddWidth)) & mask;
        return Math.floorMod(rowShift + laps * widthX, periodX);
    }

    private static long inverseOfOdd(long odd) {
        long inverse = odd;
        for (int step = 0; step < INVERSE_NEWTON_STEPS; step++) {
            inverse *= 2 - odd * inverse;
        }

        return inverse;
    }
}
