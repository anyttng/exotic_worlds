package com.exoticworlds.compat.eminus;

import org.jspecify.annotations.Nullable;

import com.eminus.ingest.saved.SavedChunk;

import net.minecraft.world.level.LevelReader;

public interface SavedChunkOwner {
    @Nullable LevelReader exotic_worlds$getOwner();

    void exotic_worlds$setOwner(@Nullable LevelReader owner);

    static void stamp(@Nullable SavedChunk chunk, LevelReader owner) {
        if (chunk != null) {
            ((SavedChunkOwner) (Object) chunk).exotic_worlds$setOwner(owner);
        }
    }

    static @Nullable LevelReader getOwner(SavedChunk chunk) {
        return ((SavedChunkOwner) (Object) chunk).exotic_worlds$getOwner();
    }
}
