package com.exoticworlds.engine.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.world.level.ChunkPos;

public final class RegionReach {
    public static @Nullable ChunkPos nearestPastTheReach(@Nullable WorldFold fold, ChunkPos center, int reach,
            int chunkX, int chunkZ) {
        if (center.getChessboardDistance(chunkX, chunkZ) < reach) {
            return null;
        }

        ChunkPos nearest = NearestCopy.toward(fold, center, new ChunkPos(chunkX, chunkZ));
        return nearest.x == chunkX && nearest.z == chunkZ ? null : nearest;
    }

    private RegionReach() {
    }
}
