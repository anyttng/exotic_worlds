package com.exoticworlds.compat.preview;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;

// mc/1.21: FreeTerraForged and ReTerraForged previews pick the spawn through it.
public final class SpawnPick {
    private static final int CHUNK_CENTER = CoordinateConstants.CHUNK_WIDTH / 2;

    public static SpawnPoint at(ToroidalShape shape, BlockPos picked) {
        BlockPos inside = shape.fold(picked);
        return new SpawnPoint(chunkInBounds(shape, Direction.Axis.X, inside.getX()),
                chunkInBounds(shape, Direction.Axis.Z, inside.getZ()));
    }

    public static BlockPos pinOf(SpawnPoint spawn) {
        return new BlockPos(spawn.blockX() + CHUNK_CENTER, 0, spawn.blockZ() + CHUNK_CENTER);
    }

    private static int chunkInBounds(ToroidalShape shape, Direction.Axis axis, int block) {
        int chunkWidth = shape.loops(axis) ? shape.widthChunks(axis) : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
        return Mth.clamp(SectionPos.blockToSectionCoord(block), WorldLoopSizes.minSpawnChunk(chunkWidth),
                WorldLoopSizes.maxSpawnChunk(chunkWidth));
    }

    private SpawnPick() {
    }
}
