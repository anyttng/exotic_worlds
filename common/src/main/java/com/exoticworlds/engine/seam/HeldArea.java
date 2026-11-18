package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.ChunkPos;

// mc/1.21: Vista's zone payload rewriter builds one per zone it sends.
public record HeldArea(ChunkPos seat, int radius) {
    public @Nullable ChunkPos copyOf(WorldFold fold, ChunkPos chunkPos) {
        ChunkPos copy = fold.nearestCopy(seat, chunkPos);
        return seat.getChessboardDistance(copy) <= radius ? copy : null;
    }
}
