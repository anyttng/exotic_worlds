package com.exoticworlds.compat.wover;

import java.util.Arrays;

public final class OpenSimplexQuantiles {
    static final double[] STAND_IN_2D = {
            0.0, 0.0096, 0.0225, 0.0371, 0.0522, 0.0680, 0.0842, 0.1006, 0.1171, 0.1337, 0.1503, 0.1670, 0.1840,
            0.2018, 0.2200, 0.2392, 0.2588, 0.2792, 0.2999, 0.3202, 0.3418, 0.3649, 0.3892, 0.4154, 0.4444, 0.4755,
            0.5085, 0.5454, 0.5851, 0.6237, 0.6742, 0.7525, 0.8454, 1.0009, 1.1156
    };

    static final double[] OPEN_SIMPLEX_2D = {
            0.0, 0.0193, 0.0382, 0.0571, 0.0757, 0.0942, 0.1128, 0.1315, 0.1504, 0.1690, 0.1879, 0.2069, 0.2256,
            0.2447, 0.2635, 0.2822, 0.3008, 0.3190, 0.3374, 0.3562, 0.3750, 0.3936, 0.4123, 0.4307, 0.4501, 0.4721,
            0.4972, 0.5256, 0.5576, 0.5939, 0.6406, 0.6998, 0.7654, 0.8286, 0.8615
    };

    static final double[] STAND_IN_3D = {
            0.0, 0.0126, 0.0254, 0.0383, 0.0514, 0.0649, 0.0785, 0.0924, 0.1065, 0.1207, 0.1351, 0.1499, 0.1649,
            0.1801, 0.1955, 0.2112, 0.2272, 0.2436, 0.2603, 0.2774, 0.2950, 0.3133, 0.3325, 0.3527, 0.3746, 0.3986,
            0.4256, 0.4559, 0.4900, 0.5296, 0.5760, 0.6449, 0.7356, 0.8794, 0.9900
    };

    static final double[] OPEN_SIMPLEX_3D = {
            0.0, 0.0143, 0.0288, 0.0433, 0.0578, 0.0724, 0.0871, 0.1018, 0.1165, 0.1314, 0.1465, 0.1617, 0.1770,
            0.1925, 0.2083, 0.2242, 0.2404, 0.2568, 0.2736, 0.2909, 0.3085, 0.3267, 0.3454, 0.3648, 0.3852, 0.4065,
            0.4292, 0.4539, 0.4813, 0.5133, 0.5535, 0.6126, 0.6903, 0.7900, 0.8571
    };

    public static double matched(double standIn) {
        return map(standIn, STAND_IN_2D, OPEN_SIMPLEX_2D);
    }

    public static double matched3d(double standIn) {
        return map(standIn, STAND_IN_3D, OPEN_SIMPLEX_3D);
    }

    private static double map(double value, double[] from, double[] to) {
        double magnitude = Math.abs(value);
        int last = from.length - 1;
        if (magnitude >= from[last]) {
            return Math.copySign(magnitude * to[last] / from[last], value);
        }

        int found = Arrays.binarySearch(from, magnitude);
        if (found >= 0) {
            return Math.copySign(to[found], value);
        }

        int upper = -found - 1;
        int lower = upper - 1;
        double share = (magnitude - from[lower]) / (from[upper] - from[lower]);
        return Math.copySign(to[lower] + share * (to[upper] - to[lower]), value);
    }

    private OpenSimplexQuantiles() {
    }
}
