package com.exoticworlds.core;

import java.lang.reflect.Proxy;

import net.minecraft.core.Direction;

public final class CuboidFixtures {
    public static final int MIN_CHUNK_X = -3;
    public static final int MIN_CHUNK_Z = 5;

    private static final String ATLAS_MEMBER = "cuboidAtlas";

    private static final int[][] CHUNK_SIZES = {{1, 1, 1}, {3, 5, 2}, {7, 1, 2}, {16, 24, 40}};

    public static CuboidAtlas[] atlases() {
        CuboidAtlas[] atlases = new CuboidAtlas[CHUNK_SIZES.length];
        for (int i = 0; i < atlases.length; i++) {
            int[] size = CHUNK_SIZES[i];
            atlases[i] = CuboidAtlas.of(size[0], size[1], size[2], MIN_CHUNK_X, MIN_CHUNK_Z);
        }

        return atlases;
    }

    public static CuboidFace face(CuboidAtlas atlas, CuboidFace.Side side) {
        return atlas.faces().stream().filter(face -> face.side() == side).findFirst().orElseThrow();
    }

    public static boolean runsAlongZ(Direction side) {
        return side == Direction.WEST || side == Direction.EAST;
    }

    public static int edgeBlocks(CuboidFace face, Direction side) {
        return runsAlongZ(side) ? face.depthBlocks() : face.widthBlocks();
    }

    public static int acrossBlocks(CuboidFace face, Direction side) {
        return runsAlongZ(side) ? face.widthBlocks() : face.depthBlocks();
    }

    public static double[] pointBy(CuboidFace face, Direction side, double along, double outward) {
        return switch (side) {
            case WEST -> new double[] {face.minBlockX() - outward, face.minBlockZ() + along};
            case EAST -> new double[] {face.minBlockX() + face.widthBlocks() + outward, face.minBlockZ() + along};
            case NORTH -> new double[] {face.minBlockX() + along, face.minBlockZ() - outward};
            case SOUTH -> new double[] {face.minBlockX() + along, face.minBlockZ() + face.depthBlocks() + outward};
            default -> throw new IllegalArgumentException("A face has no " + side + " edge");
        };
    }

    public static double[] carried(SeamTransform transform, double[] flat) {
        return new double[] {transform.applyX(flat[0], flat[1]), transform.applyZ(flat[0], flat[1])};
    }

    public static double[] turned(SeamTransform transform, double[] vector) {
        return new double[] {
                transform.applyX(vector[0], vector[1]) - transform.applyX(0.0, 0.0),
                transform.applyZ(vector[0], vector[1]) - transform.applyZ(0.0, 0.0)};
    }

    public static WorldFold cuboidFold(CuboidAtlas atlas) {
        return (WorldFold) Proxy.newProxyInstance(WorldFold.class.getClassLoader(), new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals(ATLAS_MEMBER)) {
                        return atlas;
                    }

                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private CuboidFixtures() {
    }
}
