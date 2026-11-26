package com.exoticworlds.compat.preview;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.world.WorldLoopSizes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;

// mc/1.21: FreeTerraForged and ReTerraForged previews pick the spawn through it.
public final class SpawnPick {
    private static final int CHUNK_CENTER = CoordinateConstants.CHUNK_WIDTH_BLOCKS / 2;

    public static SpawnPoint at(WorldFold fold, BlockPos picked) {
        BlockPos inside = fold.fold(picked);
        return new SpawnPoint(chunkInBounds(fold, Direction.Axis.X, inside.getX()),
                chunkInBounds(fold, Direction.Axis.Z, inside.getZ()));
    }

    public static BlockPos pinOf(SpawnPoint spawn) {
        return new BlockPos(spawn.getBlockX() + CHUNK_CENTER, 0, spawn.getBlockZ() + CHUNK_CENTER);
    }

    private static int chunkInBounds(WorldFold fold, Direction.Axis axis, int block) {
        int widthChunks = fold.isLooped(axis) ? fold.getWidthChunks(axis) : WorldLoopSizes.ENDLESS_AXIS_WIDTH;
        return Mth.clamp(SectionPos.blockToSectionCoord(block), WorldLoopSizes.minSpawnChunk(widthChunks),
                WorldLoopSizes.maxSpawnChunk(widthChunks));
    }

    private SpawnPick() {
    }
}
