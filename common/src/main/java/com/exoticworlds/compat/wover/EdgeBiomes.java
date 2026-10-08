package com.exoticworlds.compat.wover;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker.PickableBiome;

public final class EdgeBiomes {
    private static final int HEX_RING_POINTS = 8;

    private static final float HEX_RING_TURNS = 4.0F;

    private static final float[] HEX_RING_X = new float[HEX_RING_POINTS];

    private static final float[] HEX_RING_Z = new float[HEX_RING_POINTS];

    private static final double SQUARE_NEIGHBOUR_BLOCKS = 1.0;

    static {
        for (int point = 0; point < HEX_RING_POINTS; point++) {
            float angle = point / HEX_RING_TURNS * (float) Math.PI;
            HEX_RING_X[point] = (float) Math.sin(angle);
            HEX_RING_Z[point] = (float) Math.cos(angle);
        }
    }

    public static PickableBiome hex(LapMap<PickableBiome> map, double x, double z) {
        PickableBiome biome = map.biomeAt(x, z);
        PickableBiome edge = biome.edge;
        int edgeSize = biome.edgeSize;
        if (edge == null && biome.getParentBiome() != null) {
            edge = biome.getParentBiome().edge;
            edgeSize = biome.getParentBiome().edgeSize;
        }

        if (edge == null) {
            return biome;
        }

        double radius = radius(map, edgeSize);
        for (int point = 0; point < HEX_RING_POINTS; point++) {
            if (!map.biomeAt(x + radius * HEX_RING_X[point], z + radius * HEX_RING_Z[point]).isSame(biome)) {
                return edge;
            }
        }

        return biome;
    }

    public static PickableBiome square(LapMap<PickableBiome> map, double x, double z) {
        PickableBiome biome = map.biomeAt(x, z);
        PickableBiome parent = biome.getParentBiome();
        if (biome.getEdge() == null && (parent == null || parent.getEdge() == null)) {
            return biome;
        }

        PickableBiome search = parent != null ? parent : biome;
        double radius = radius(map, search.edgeSize);
        double step = SQUARE_NEIGHBOUR_BLOCKS / map.factor();
        boolean edge = !search.isSame(map.biomeAt(x + radius, z))
                || !search.isSame(map.biomeAt(x - radius, z))
                || !search.isSame(map.biomeAt(x, z + radius))
                || !search.isSame(map.biomeAt(x, z - radius))
                || !search.isSame(map.biomeAt(x - step, z - step))
                || !search.isSame(map.biomeAt(x - step, z + step))
                || !search.isSame(map.biomeAt(x + step, z - step))
                || !search.isSame(map.biomeAt(x + step, z + step));
        return edge ? search.getEdge() : biome;
    }

    private static double radius(LapMap<?> map, int edgeSize) {
        return edgeSize / map.factor();
    }

    private EdgeBiomes() {
    }
}
