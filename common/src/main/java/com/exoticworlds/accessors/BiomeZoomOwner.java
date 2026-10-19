package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.LevelReader;

public interface BiomeZoomOwner {
    @Nullable
    LevelReader exotic_worlds$owner();

    void exotic_worlds$setOwner(@Nullable LevelReader owner);
}
