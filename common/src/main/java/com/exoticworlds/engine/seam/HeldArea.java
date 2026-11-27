package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.ChunkPos;

// mc/1.21: Vista's zone payload rewriter builds one per zone it sends.
public record HeldArea(ChunkPos seat, int radiusChunks) {
    public @Nullable DeckTransformation moveOf(ClientPosition clientPosition, WorldFold fold, ChunkPos chunkPos) {
        DeckTransformation move = clientPosition.nearestCopyTransformation(fold, seat, chunkPos);
        return seat.getChessboardDistance(move.apply(chunkPos)) <= radiusChunks ? move : null;
    }
}
