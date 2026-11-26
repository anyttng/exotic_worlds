package com.exoticworlds.api.v2.feature;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.ExperimentalV2;

import net.minecraft.nbt.CompoundTag;

/**
 * The moments a chunk is written to disk, read back and dropped from memory, for a feature that keeps data of its own
 * with each chunk. The data is saved inside the chunk under the
 * feature's id with {@code exotic_worlds:} before it.
 */
@ExperimentalV2
public interface ChunkDataMomentV2 extends LevelMomentV2 {
    /** The data to save with the chunk, or {@code null} to save none. */
    @Nullable CompoundTag atChunkSave(ChunkSlotV2 slot);

    /** Called for a chunk read from disk with the data saved for it. */
    void atChunkLoad(ChunkV2 chunk, CompoundTag data);

    /** Called once the chunk is saved and no longer in memory, to let go of what the feature holds for it. */
    void atChunkRelease(ChunkSlotV2 slot);
}
