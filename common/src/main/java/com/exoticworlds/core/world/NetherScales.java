package com.exoticworlds.core.world;

import java.util.ArrayList;
import java.util.List;

import com.google.common.math.IntMath;

import net.minecraft.core.Direction;

public final class NetherScales {
    public static final int DEFAULT = 8;

    private static final int SMALLEST = 1;

    public static List<Integer> allowedFor(FlatShape overworld) {
        WorldLoopBounds bounds = overworld.bounds();
        Direction.Axis looped = bounds.isLooped(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
        int loopedWidthChunks = bounds.getWidthChunks(looped);
        int xWidthChunks = bounds.isLooped(Direction.Axis.X) ? bounds.getWidthChunks(Direction.Axis.X) : loopedWidthChunks;
        int zWidthChunks = bounds.isLooped(Direction.Axis.Z) ? bounds.getWidthChunks(Direction.Axis.Z) : loopedWidthChunks;
        return allowedFor(xWidthChunks, zWidthChunks, overworld.skewChunks());
    }

    public static List<Integer> allowedFor(int overworldXWidthChunks, int overworldZWidthChunks,
            int overworldSkewChunks) {
        int maxScale = Math.min(overworldXWidthChunks, overworldZWidthChunks) / WorldLoopSizes.MIN_WIDTH_CHUNKS;
        if (maxScale < SMALLEST) {
            return List.of(SMALLEST);
        }

        int widths = IntMath.gcd(overworldXWidthChunks, overworldZWidthChunks);
        List<Integer> scales = new ArrayList<>();
        for (int divisor : Divisors.of(IntMath.gcd(widths, Math.abs(overworldSkewChunks)))) {
            if (divisor > maxScale) {
                break;
            }

            scales.add(divisor);
        }

        return scales;
    }

    public static int normalize(int scale, List<Integer> allowed) {
        int fallen = allowed.get(0);
        for (int candidate : allowed) {
            if (candidate > scale) {
                break;
            }

            fallen = candidate;
        }

        return fallen;
    }

    private NetherScales() {
    }
}
