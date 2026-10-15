package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

public interface TrackNodeKeyFold {
    // Only on a freshly built location: folding one already filed as a TrackGraph key strands that entry.
    void toroidal$foldNodeKey(@Nullable Level level);
}
