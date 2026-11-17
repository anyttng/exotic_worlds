package com.exoticworlds.compat.eminus;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.LevelReader;

public interface SavedChunkOwner {
    @Nullable LevelReader exotic_worlds$owner();

    void exotic_worlds$setOwner(@Nullable LevelReader owner);

    static void stamp(@Nullable Object chunk, LevelReader owner) {
        if (chunk instanceof SavedChunkOwner owned) {
            owned.exotic_worlds$setOwner(owner);
        }
    }

    static @Nullable LevelReader ownerOf(Object chunk) {
        return chunk instanceof SavedChunkOwner owned ? owned.exotic_worlds$owner() : null;
    }
}
