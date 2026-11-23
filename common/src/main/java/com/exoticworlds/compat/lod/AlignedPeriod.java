package com.exoticworlds.compat.lod;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;

public record AlignedPeriod(long xBlocks, long zBlocks, long xShiftBlocks) {
    private static final long NO_PERIOD = 0L;
    private static final int INVERSE_NEWTON_STEPS = 5;

    public static AlignedPeriod of(WorldFold fold, int skewBlocks, int alignExponent) {
        long x = fold.loops(Direction.Axis.X) ? blocks(fold.widthBlocks(Direction.Axis.X), alignExponent) : NO_PERIOD;
        if (!fold.loops(Direction.Axis.Z)) {
            return new AlignedPeriod(x, NO_PERIOD, 0L);
        }

        int widthZ = fold.widthBlocks(Direction.Axis.Z);
        int lapsExponent = alignExponent - Math.min(alignExponent, Integer.numberOfTrailingZeros(widthZ));
        if (skewBlocks == 0) {
            return new AlignedPeriod(x, ((long) widthZ) << lapsExponent, 0L);
        }

        int widthX = fold.widthBlocks(Direction.Axis.X);
        int alignX = Math.min(alignExponent, Integer.numberOfTrailingZeros(widthX));
        lapsExponent = Math.max(lapsExponent, alignX - Integer.numberOfTrailingZeros(skewBlocks));
        long rowShift = ((long) skewBlocks) << lapsExponent;
        return new AlignedPeriod(x, ((long) widthZ) << lapsExponent, shift(widthX, alignExponent, rowShift, x));
    }

    public static long blocks(int worldWidth, int alignExponent) {
        int gcd = 1 << Math.min(Integer.numberOfTrailingZeros(worldWidth), alignExponent);
        return (long) (worldWidth / gcd) << alignExponent;
    }

    public long foldX(WorldFold fold, long blockX, long blockZ) {
        long x = blockX;
        if (xShiftBlocks != 0L) {
            x -= Math.floorDiv(blockZ - fold.minBlock(Direction.Axis.Z), zBlocks) * xShiftBlocks;
        }

        if (fold.loops(Direction.Axis.X)) {
            x -= Math.floorDiv(x - fold.minBlock(Direction.Axis.X), xBlocks) * xBlocks;
        }

        return x;
    }

    public long foldZ(WorldFold fold, long blockZ) {
        if (!fold.loops(Direction.Axis.Z)) {
            return blockZ;
        }

        return blockZ - Math.floorDiv(blockZ - fold.minBlock(Direction.Axis.Z), zBlocks) * zBlocks;
    }

    public int laps(WorldFold fold, Direction.Axis axis) {
        if (!fold.loops(axis)) {
            return 1;
        }

        return (int) ((axis == Direction.Axis.X ? xBlocks : zBlocks) / fold.widthBlocks(axis));
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
