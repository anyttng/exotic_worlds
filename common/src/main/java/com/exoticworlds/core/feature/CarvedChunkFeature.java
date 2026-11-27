package com.exoticworlds.core.feature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface CarvedChunkFeature extends LevelFeature {
    void atCarved(ServerLevel level, ChunkAccess chunk);
}
