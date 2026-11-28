package com.exoticworlds.compat.wover;

import org.jspecify.annotations.Nullable;

public final class EdgeBiomes {
    private static final int HEX_RING_POINTS = 8;

    private static final float HEX_RING_TURNS = 4.0F;

    private static final float[] HEX_RING_X = new float[HEX_RING_POINTS];

    private static final float[] HEX_RING_Z = new float[HEX_RING_POINTS];

    private static final double SQUARE_NEIGHBOR_BLOCKS = 1.0;

    static {
        for (int point = 0; point < HEX_RING_POINTS; point++) {
            float angle = point / HEX_RING_TURNS * (float) Math.PI;
            HEX_RING_X[point] = (float) Math.sin(angle);
            HEX_RING_Z[point] = (float) Math.cos(angle);
        }
    }

    public static <T> T hex(LapMap<T> map, PickedBiomeTraits<T> traits, double x, double z) {
        T biome = map.biomeAt(x, z);
        T edge = traits.getEdge(biome);
        int edgeSize = traits.getEdgeSize(biome);
        T parent = traits.getParent(biome);
        if (edge == null && parent != null) {
            edge = traits.getEdge(parent);
            edgeSize = traits.getEdgeSize(parent);
        }

        if (edge == null) {
            return biome;
        }

        double radius = radius(map, edgeSize);
        for (int point = 0; point < HEX_RING_POINTS; point++) {
            T ringBiome = map.biomeAt(x + radius * HEX_RING_X[point], z + radius * HEX_RING_Z[point]);
            if (!traits.isSame(ringBiome, biome)) {
                return edge;
            }
        }

        return biome;
    }

    public static <T> @Nullable T square(LapMap<T> map, PickedBiomeTraits<T> traits, double x, double z) {
        T biome = map.biomeAt(x, z);
        T parent = traits.getParent(biome);
        if (traits.getEdge(biome) == null && (parent == null || traits.getEdge(parent) == null)) {
            return biome;
        }

        T search = parent != null ? parent : biome;
        double radius = radius(map, traits.getEdgeSize(search));
        double step = SQUARE_NEIGHBOR_BLOCKS / map.getFactor();
        boolean isEdge = !traits.isSame(search, map.biomeAt(x + radius, z))
                || !traits.isSame(search, map.biomeAt(x - radius, z))
                || !traits.isSame(search, map.biomeAt(x, z + radius))
                || !traits.isSame(search, map.biomeAt(x, z - radius))
                || !traits.isSame(search, map.biomeAt(x - step, z - step))
                || !traits.isSame(search, map.biomeAt(x - step, z + step))
                || !traits.isSame(search, map.biomeAt(x + step, z - step))
                || !traits.isSame(search, map.biomeAt(x + step, z + step));
        return isEdge ? traits.getEdge(search) : biome;
    }

    private static double radius(LapMap<?> map, int edgeSize) {
        return edgeSize / map.getFactor();
    }

    private EdgeBiomes() {
    }
}
