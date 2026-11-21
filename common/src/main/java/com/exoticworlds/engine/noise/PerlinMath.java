package com.exoticworlds.engine.noise;

import net.minecraft.util.Mth;

public final class PerlinMath {
    public static final double NO_FUDGE = 0.0;

    private static final int[][] GRADIENT = {
            {1, 1, 0},
            {-1, 1, 0},
            {1, -1, 0},
            {-1, -1, 0},
            {1, 0, 1},
            {-1, 0, 1},
            {1, 0, -1},
            {-1, 0, -1},
            {0, 1, 1},
            {0, -1, 1},
            {0, 1, -1},
            {0, -1, -1},
            {1, 1, 0},
            {0, -1, 1},
            {-1, 1, 0},
            {0, -1, -1}
    };

    private static final int GRADIENT_MASK = 15;

    private static final float FUDGE_EPSILON = 1.0E-7F;

    public static int[] gradient(int hash) {
        return GRADIENT[hash & GRADIENT_MASK];
    }

    public static double fudgeY(double originalY, double yRelative, double fudgeYScale) {
        double fudgeLimit = originalY >= 0.0 && originalY < yRelative ? originalY : yRelative;
        return Mth.floor(fudgeLimit / fudgeYScale + FUDGE_EPSILON) * fudgeYScale;
    }

    public static int p(byte[] permutations, long index) {
        return permutations[(int) (index & 0xFFL)] & 0xFF;
    }

    private PerlinMath() {
    }
}
