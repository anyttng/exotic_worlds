package com.exoticworlds.api.v2.feature;

import com.exoticworlds.api.v2.ExperimentalV2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** The place of one chunk in a loaded level, whether or not the chunk itself is in memory. */
@ExperimentalV2
public interface ChunkSlotV2 extends MomentV2 {
    /** The level. */
    ServerLevel getLevel();

    /** The chunk's position. */
    ChunkPos getPos();
}
