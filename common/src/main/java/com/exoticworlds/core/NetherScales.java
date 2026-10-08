package com.exoticworlds.core;

import java.util.ArrayList;
import java.util.List;

import com.google.common.math.IntMath;

public final class NetherScales {
    public static final int DEFAULT = 8;

    private static final int SMALLEST = 1;

    public static List<Integer> allowedFor(int overworldChunkWidth) {
        return allowedFor(overworldChunkWidth, overworldChunkWidth);
    }

    public static List<Integer> allowedFor(int overworldXChunkWidth, int overworldZChunkWidth) {
        int maxScale = Math.min(overworldXChunkWidth, overworldZChunkWidth) / WorldLoopSizes.MIN_CHUNK_WIDTH;
        if (maxScale < SMALLEST) {
            return List.of(SMALLEST);
        }

        List<Integer> scales = new ArrayList<>();
        for (int divisor : Divisors.of(IntMath.gcd(overworldXChunkWidth, overworldZChunkWidth))) {
            if (divisor > maxScale) {
                break;
            }

            scales.add(divisor);
        }

        return scales;
    }

    public static int normalize(int scale, int overworldChunkWidth) {
        return normalize(scale, allowedFor(overworldChunkWidth));
    }

    public static int normalize(int scale, int overworldXChunkWidth, int overworldZChunkWidth) {
        return normalize(scale, allowedFor(overworldXChunkWidth, overworldZChunkWidth));
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

    public static int next(int scale, int overworldChunkWidth) {
        return next(scale, overworldChunkWidth, overworldChunkWidth);
    }

    public static int next(int scale, int overworldXChunkWidth, int overworldZChunkWidth) {
        List<Integer> allowed = allowedFor(overworldXChunkWidth, overworldZChunkWidth);
        int index = allowed.indexOf(scale);
        return allowed.get((index + 1) % allowed.size());
    }

    public static int netherChunkWidth(int overworldChunkWidth, int scale) {
        return overworldChunkWidth / scale;
    }

    private NetherScales() {
    }
}
