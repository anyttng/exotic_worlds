package com.exoticworlds.engine.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.world.level.ChunkPos;

public final class RegionReach {
    public static @Nullable ChunkPos nearestPastTheReach(@Nullable WorldFold fold, ChunkPos center, int reachChunks,
            int chunkX, int chunkZ) {
        if (center.getChessboardDistance(chunkX, chunkZ) < reachChunks) {
            return null;
        }

        ChunkPos nearest = NearestCopy.toward(fold, center, new ChunkPos(chunkX, chunkZ));
        return nearest.x() == chunkX && nearest.z() == chunkZ ? null : nearest;
    }

    private RegionReach() {
    }
}
