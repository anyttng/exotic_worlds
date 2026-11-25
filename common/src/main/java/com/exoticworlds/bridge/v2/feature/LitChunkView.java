package com.exoticworlds.bridge.v2.feature;

import java.util.concurrent.Executor;

import com.exoticworlds.api.v2.feature.LitChunkV2;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;

final class LitChunkView extends ChunkView implements LitChunkV2 {
    private final StaticCache2D<GenerationChunkHolder> neighbors;
    private final Executor mainThread;

    LitChunkView(ServerLevel level, ChunkAccess chunk, StaticCache2D<GenerationChunkHolder> neighbors,
            Executor mainThread) {
        super(level, chunk);
        this.neighbors = neighbors;
        this.mainThread = mainThread;
    }

    @Override
    public StaticCache2D<GenerationChunkHolder> getNeighbors() {
        return this.neighbors;
    }

    @Override
    public Executor getMainThread() {
        return this.mainThread;
    }
}
