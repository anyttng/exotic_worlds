package com.exoticworlds.api.v2.feature;

import com.exoticworlds.api.v2.ExperimentalV2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

/** One chunk of a loaded level at a step of its generation or its loading. */
@ExperimentalV2
public interface ChunkV2 extends MomentV2 {
    /** The level the chunk belongs to. */
    ServerLevel getLevel();

    /** The chunk. Its blocks may be read and written. */
    ChunkAccess getChunk();
}
