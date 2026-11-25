package com.exoticworlds.bridge.v2.feature;

import com.exoticworlds.api.v2.feature.ChunkV2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

class ChunkView extends MomentView implements ChunkV2 {
    private final ServerLevel level;
    private final ChunkAccess chunk;

    ChunkView(ServerLevel level, ChunkAccess chunk) {
        super(level);
        this.level = level;
        this.chunk = chunk;
    }

    @Override
    public final ServerLevel getLevel() {
        return this.level;
    }

    @Override
    public final ChunkAccess getChunk() {
        return this.chunk;
    }
}
