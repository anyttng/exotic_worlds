package com.exoticworlds.api.v2.feature;

import java.util.concurrent.Executor;

import com.exoticworlds.api.v2.ExperimentalV2;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;

/** A chunk whose light is done, with the chunks around it. */
@ExperimentalV2
public interface LitChunkV2 extends ChunkV2 {
    /** The chunk and its neighbors as the generation step holds them. */
    StaticCache2D<GenerationChunkHolder> getNeighbors();

    /** Runs a task on the server thread, for what only that thread may touch. */
    Executor getMainThread();
}
