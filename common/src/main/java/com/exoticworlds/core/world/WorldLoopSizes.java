package com.exoticworlds.core.world;

import net.minecraft.world.level.border.WorldBorder;

public final class WorldLoopSizes {
    private static final int MIN_PLAYABLE_VIEW_DISTANCE_CHUNKS = 5;

    private static final int MIN_RADIUS_CHUNKS =
            CoordinateConstants.VIEW_DISTANCE_MARGIN_CHUNKS + MIN_PLAYABLE_VIEW_DISTANCE_CHUNKS;

    private static final int MAX_RADIUS_CHUNKS = (int) (WorldBorder.MAX_CENTER_COORDINATE / CoordinateConstants.CHUNK_WIDTH_BLOCKS);

    public static final int MIN_WIDTH_CHUNKS = MIN_RADIUS_CHUNKS * 2;
    public static final int MAX_WIDTH_CHUNKS = MAX_RADIUS_CHUNKS * 2;

    public static final int MAX_CUBOID_EDGE_CHUNKS = MAX_WIDTH_CHUNKS / 4;

    public static final int DEFAULT_WIDTH_CHUNKS = 32;

    public static final int ENDLESS_AXIS_WIDTH = 0;

    public static final int END_MIN_WIDTH_CHUNKS = 192;
    public static final int END_DEFAULT_WIDTH_CHUNKS = 256;

    public static String describe(int widthChunks) {
        return widthChunks + " chunks (" + widthChunks * CoordinateConstants.CHUNK_WIDTH_BLOCKS + " blocks)";
    }

    public static boolean isInRange(int widthChunks) {
        return widthChunks >= MIN_WIDTH_CHUNKS && widthChunks <= MAX_WIDTH_CHUNKS;
    }

    public static boolean isEndInRange(int widthChunks) {
        return widthChunks >= END_MIN_WIDTH_CHUNKS && widthChunks <= MAX_WIDTH_CHUNKS;
    }

    public static int endWidthChunksFor(int overworldWidthChunks) {
        return Math.min(MAX_WIDTH_CHUNKS, END_MIN_WIDTH_CHUNKS + overworldWidthChunks - MIN_WIDTH_CHUNKS);
    }

    public static int getMinWidthChunks(StemTerrain stem) {
        return stem.isEndTerrain() ? END_MIN_WIDTH_CHUNKS : MIN_WIDTH_CHUNKS;
    }

    public static int getDefaultWidthChunks(StemTerrain stem) {
        return stem.isEndTerrain() ? END_DEFAULT_WIDTH_CHUNKS : DEFAULT_WIDTH_CHUNKS;
    }

    public static int minSpawnChunk(int widthChunks) {
        return widthChunks == ENDLESS_AXIS_WIDTH ? -MAX_RADIUS_CHUNKS : -(widthChunks / 2);
    }

    public static int maxSpawnChunk(int widthChunks) {
        return widthChunks == ENDLESS_AXIS_WIDTH ? MAX_RADIUS_CHUNKS : widthChunks - widthChunks / 2 - 1;
    }

    private WorldLoopSizes() {
    }
}
