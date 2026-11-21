package com.exoticworlds.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CuboidFace.BoxVector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class CuboidFixtures {
    public static final int MIN_CHUNK_X = -3;
    public static final int MIN_CHUNK_Z = 5;

    public static final int BLOCK_Y = 64;

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

    public static List<BlockPos> blocksAlong(CuboidFace face, Direction side) {
        List<BlockPos> blocks = new ArrayList<>();
        boolean alongZ = runsAlongZ(side);
        int far = acrossBlocks(face, side) - 1;
        int fixed = side == Direction.SOUTH || side == Direction.EAST ? far : 0;
        for (int along = 0; along < edgeBlocks(face, side); along++) {
            blocks.add(alongZ
                    ? new BlockPos(face.blockX(fixed), BLOCK_Y, face.blockZ(along))
                    : new BlockPos(face.blockX(along), BLOCK_Y, face.blockZ(fixed)));
        }

        return blocks;
    }

    public static Set<BoxVector> sharedSegment(CuboidFace face, BlockPos block, Direction side) {
        int localX = face.localX(block.getX());
        int localZ = face.localZ(block.getZ());
        return switch (side) {
            case NORTH -> Set.of(face.boxPoint(localX, localZ), face.boxPoint(localX + 1, localZ));
            case SOUTH -> Set.of(face.boxPoint(localX, localZ + 1), face.boxPoint(localX + 1, localZ + 1));
            case WEST -> Set.of(face.boxPoint(localX, localZ), face.boxPoint(localX, localZ + 1));
            case EAST -> Set.of(face.boxPoint(localX + 1, localZ), face.boxPoint(localX + 1, localZ + 1));
            default -> throw new IllegalArgumentException("A face has no " + side + " edge");
        };
    }

    public static WorldFold cuboidFold(CuboidAtlas atlas) {
        return new CuboidFold(atlas, GenerationOptions.DEFAULT);
    }

    private CuboidFixtures() {
    }
}
