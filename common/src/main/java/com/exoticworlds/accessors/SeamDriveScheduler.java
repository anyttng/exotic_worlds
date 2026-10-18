package com.exoticworlds.accessors;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// Filed from worker threads when a foreign task declines a cross-seam holder, drained on the server thread.
public interface SeamDriveScheduler {
    void exotic_worlds$requestDrive(GenerationChunkHolder holder, ChunkStatus status);
}
