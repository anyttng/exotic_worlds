package com.exoticworlds.core.feature;

import java.util.concurrent.Executor;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface LitChunkFeature extends LevelFeature {
    void atLit(ServerLevel level, ChunkAccess chunk, StaticCache2D<GenerationChunkHolder> chunks, Executor mainThread);
}
