package com.exoticworlds.engine.fold;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.ChunkPos;

public sealed interface ChunkViewSweep {
    int NEIGHBOR_RING = 1;

    record OverBoth(int minX, int minZ, int maxX, int maxZ) implements ChunkViewSweep {}

    record EachAlone() implements ChunkViewSweep {}

    static ChunkViewSweep between(WorldFold fold, ChunkPos previousCenter, int previousViewDistance,
            ChunkPos nextCenter, int nextViewDistance) {
        ChunkPos nearestNextCenter = fold.nearestCopy(previousCenter, nextCenter);
        int previousRadius = previousViewDistance + NEIGHBOR_RING;
        int nextRadius = nextViewDistance + NEIGHBOR_RING;

        long reach = (long) previousRadius + nextRadius;
        if (Math.abs((long) nearestNextCenter.x - previousCenter.x) > reach
                || Math.abs((long) nearestNextCenter.z - previousCenter.z) > reach) {
            return new EachAlone();
        }

        return new OverBoth(
                Math.min(previousCenter.x - previousRadius, nearestNextCenter.x - nextRadius),
                Math.min(previousCenter.z - previousRadius, nearestNextCenter.z - nextRadius),
                Math.max(previousCenter.x + previousRadius, nearestNextCenter.x + nextRadius),
                Math.max(previousCenter.z + previousRadius, nearestNextCenter.z + nextRadius));
    }
}
