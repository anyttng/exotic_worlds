package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class WaypointLapGate {
    private @Nullable ChunkPos lastClientChunk;

    public int widen(int distance, ServerPlayer receiver, BlockPos serverBlock) {
        if (WorldLoopAttachments.getWrappedFold(receiver.level()) == null) {
            return distance;
        }

        return widen(distance, receiver, ChunkPos.containing(serverBlock));
    }

    public int widen(int distance, ServerPlayer receiver, ChunkPos serverChunk) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(receiver.level());
        if (fold == null) {
            return distance;
        }

        ClientPosition mirror = ClientPosition.of(receiver);
        if (!mirror.isForDimension(receiver.level().dimension())) {
            return distance;
        }

        ChunkPos projection = mirror.nearestCopy(fold, mirror.getAnchorChunk(), serverChunk);
        ChunkPos previous = this.lastClientChunk;
        this.lastClientChunk = projection;
        if (distance > 0 || previous == null || previous.equals(projection)) {
            return distance;
        }

        return 1;
    }
}
