package com.exoticworlds.engine.fold;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.ChunkPos;

public sealed interface ChunkViewSweep {
    int NEIGHBOR_RING = 1;

    record OverBoth(int minX, int minZ, int maxX, int maxZ) implements ChunkViewSweep {}

    record EachAlone() implements ChunkViewSweep {}

    static ChunkViewSweep between(WorldFold fold, ChunkPos previousCenter, int previousViewDistanceChunks,
            ChunkPos nextCenter, int nextViewDistanceChunks) {
        ChunkPos nearestNextCenter = fold.nearestCopy(previousCenter, nextCenter);
        int previousRadiusChunks = previousViewDistanceChunks + NEIGHBOR_RING;
        int nextRadiusChunks = nextViewDistanceChunks + NEIGHBOR_RING;

        long reachChunks = (long) previousRadiusChunks + nextRadiusChunks;
        if (Math.abs((long) nearestNextCenter.x() - previousCenter.x()) > reachChunks
                || Math.abs((long) nearestNextCenter.z() - previousCenter.z()) > reachChunks) {
            return new EachAlone();
        }

        return new OverBoth(
                Math.min(previousCenter.x() - previousRadiusChunks, nearestNextCenter.x() - nextRadiusChunks),
                Math.min(previousCenter.z() - previousRadiusChunks, nearestNextCenter.z() - nextRadiusChunks),
                Math.max(previousCenter.x() + previousRadiusChunks, nearestNextCenter.x() + nextRadiusChunks),
                Math.max(previousCenter.z() + previousRadiusChunks, nearestNextCenter.z() + nextRadiusChunks));
    }
}
