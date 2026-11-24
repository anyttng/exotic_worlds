package com.exoticworlds.shape;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.WorldLoopSizes;

public enum WorldLoopPresets {
    TINY("tiny", 32, 2),
    SMALL("small", 64, 4),
    MEDIUM("medium", 128, 8),
    LARGE("large", 256, 8),
    HUGE("huge", 512, 8);

    private final String id;
    private final int widthChunks;
    private final int netherScale;

    WorldLoopPresets(String id, int widthChunks, int netherScale) {
        this.id = id;
        this.widthChunks = widthChunks;
        this.netherScale = netherScale;
    }

    public String getId() {
        return id;
    }

    public int getWidthChunks() {
        return widthChunks;
    }

    public int getNetherScale() {
        return netherScale;
    }

    public int getEndWidthChunks() {
        return WorldLoopSizes.endWidthChunksFor(widthChunks);
    }

    public int getWidthBlocks() {
        return widthChunks * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    }
}
