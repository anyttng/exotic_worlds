package com.exoticworlds.shape;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.WorldLoopSizes;

public enum WorldLoopPresets {
    TINY("tiny", 32, 2),
    SMALL("small", 64, 4),
    MEDIUM("medium", 128, 8),
    LARGE("large", 256, 8),
    HUGE("huge", 512, 8);

    private final String id;
    private final int chunkWidth;
    private final int netherScale;

    WorldLoopPresets(String id, int chunkWidth, int netherScale) {
        this.id = id;
        this.chunkWidth = chunkWidth;
        this.netherScale = netherScale;
    }

    public String id() {
        return id;
    }

    public int chunkWidth() {
        return chunkWidth;
    }

    public int netherScale() {
        return netherScale;
    }

    public int endChunkWidth() {
        return WorldLoopSizes.endChunkWidthFor(chunkWidth);
    }

    public int blockWidth() {
        return chunkWidth * CoordinateConstants.CHUNK_WIDTH;
    }
}
