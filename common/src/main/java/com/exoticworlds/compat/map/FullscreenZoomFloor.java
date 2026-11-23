package com.exoticworlds.compat.map;

import java.util.function.ToDoubleFunction;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class FullscreenZoomFloor {
    public static final int MIN_WORLD_PIXELS = 64;

    public static final int JOURNEYMAP_REGION_BLOCKS = 512;
    private static final int FTBCHUNKS_ZOOM_BLOCKS = 256;

    public static final int JOURNEYMAP_MAX_ZOOM = 16_384;

    public static final int FTBCHUNKS_MAX_ZOOM = 1_024;

    public static int journeyMapZoom(WorldFold fold) {
        return (int) maxOverLoopedAxes(fold, axis -> journeyMapZoom(fold.widthBlocks(axis)));
    }

    public static double xaeroScale(WorldFold fold, double scaleMultiplier) {
        return maxOverLoopedAxes(fold, axis -> xaeroScale(fold.widthBlocks(axis), scaleMultiplier));
    }

    public static int ftbChunksZoom(WorldFold fold) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.loops(axis)) {
                floor = Math.max(floor, ftbChunksZoom(fold.widthBlocks(axis)));
            }
        }

        return floor;
    }

    public static int journeyMapCoverZoom(WorldFold fold, int windowWidth, int windowHeight) {
        return (int) maxOverLoopedAxes(fold,
                axis -> journeyMapCoverZoom(fold.widthBlocks(axis), windowSide(axis, windowWidth, windowHeight)));
    }

    public static int ftbChunksCoverZoom(WorldFold fold, int windowWidth, int windowHeight) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.loops(axis)) {
                floor = Math.max(floor, ftbChunksCoverZoom(fold.widthBlocks(axis), windowSide(axis, windowWidth, windowHeight)));
            }
        }

        return floor;
    }

    public static double xaeroCoverScale(WorldFold fold, double scaleMultiplier, int windowWidth, int windowHeight) {
        return maxOverLoopedAxes(fold,
                axis -> xaeroCoverScale(fold.widthBlocks(axis), scaleMultiplier, windowSide(axis, windowWidth, windowHeight)));
    }

    // mc/26.2: Simple Atlas holds its SINGLE view at this zoom.
    public static float simpleAtlasCoverZoom(WorldFold fold, float viewWidth, float viewHeight, int blocksPerTile,
            float tilePixels) {
        return (float) maxOverLoopedAxes(fold, axis -> simpleAtlasCoverZoom(fold.widthBlocks(axis),
                windowSide(axis, viewWidth, viewHeight), blocksPerTile, tilePixels));
    }

    public static float ultimateMapAtlasesZoom(WorldFold fold, float viewWidth, int mapBlocks) {
        return (float) minOverLoopedAxes(fold,
                axis -> ultimateMapAtlasesZoom(fold.widthBlocks(axis), viewWidth, mapBlocks));
    }

    public static float ultimateMapAtlasesCoverZoom(WorldFold fold, float viewWidth, float viewHeight,
            int mapBlocks) {
        return (float) minOverLoopedAxes(fold, axis -> ultimateMapAtlasesCoverZoom(fold.widthBlocks(axis), viewWidth,
                windowSide(axis, viewWidth, viewHeight), mapBlocks));
    }

    public static float ultraTerraForgedZoom(WorldFold fold, int widgetPixels, int previewPixels) {
        return (float) minOverLoopedAxes(fold,
                axis -> ultraTerraForgedZoom(fold.widthBlocks(axis), widgetPixels, previewPixels));
    }

    public static float ultraTerraForgedCoverZoom(WorldFold fold, int previewPixels) {
        return (float) minOverLoopedAxes(fold,
                axis -> ultraTerraForgedCoverZoom(fold.widthBlocks(axis), previewPixels));
    }

    private static double maxOverLoopedAxes(WorldFold fold, ToDoubleFunction<Direction.Axis> floorOf) {
        double floor = 0.0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.loops(axis)) {
                floor = Math.max(floor, floorOf.applyAsDouble(axis));
            }
        }

        return floor;
    }

    private static double minOverLoopedAxes(WorldFold fold, ToDoubleFunction<Direction.Axis> ceilingOf) {
        double ceiling = Double.MAX_VALUE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.loops(axis)) {
                ceiling = Math.min(ceiling, ceilingOf.applyAsDouble(axis));
            }
        }

        return ceiling;
    }

    public static int journeyMapSeatedZoom(int zoom, int floor, MapCopies copies) {
        if (zoom >= floor) {
            return zoom;
        }

        return copies == MapCopies.SINGLE ? floor : Mth.smallestEncompassingPowerOfTwo(floor);
    }

    static int journeyMapCoverZoom(int widthBlocks, int windowPixels) {
        return Math.min(Math.ceilDiv(windowPixels * JOURNEYMAP_REGION_BLOCKS, widthBlocks), JOURNEYMAP_MAX_ZOOM);
    }

    static int ftbChunksCoverZoom(int widthBlocks, int windowPixels) {
        return Math.min(Math.ceilDiv(windowPixels * FTBCHUNKS_ZOOM_BLOCKS, widthBlocks), FTBCHUNKS_MAX_ZOOM);
    }

    static double xaeroCoverScale(int widthBlocks, double scaleMultiplier, int windowPixels) {
        return windowPixels / (widthBlocks * scaleMultiplier);
    }

    static float simpleAtlasCoverZoom(int widthBlocks, float viewPixels, int blocksPerTile, float tilePixels) {
        return viewPixels * blocksPerTile / (tilePixels * widthBlocks);
    }

    static float ultimateMapAtlasesZoom(int widthBlocks, float viewWidth, int mapBlocks) {
        return widthBlocks * viewWidth / (MIN_WORLD_PIXELS * mapBlocks);
    }

    static float ultimateMapAtlasesCoverZoom(int widthBlocks, float viewWidth, float sidePixels, int mapBlocks) {
        return widthBlocks * viewWidth / (sidePixels * mapBlocks);
    }

    static double ultraTerraForgedZoom(int widthBlocks, int widgetPixels, int previewPixels) {
        return (double) widthBlocks * widgetPixels / ((double) previewPixels * MIN_WORLD_PIXELS);
    }

    static double ultraTerraForgedCoverZoom(int widthBlocks, int previewPixels) {
        return (double) widthBlocks / previewPixels;
    }

    private static int windowSide(Direction.Axis axis, int windowWidth, int windowHeight) {
        return axis == Direction.Axis.X ? windowWidth : windowHeight;
    }

    private static float windowSide(Direction.Axis axis, float windowWidth, float windowHeight) {
        return axis == Direction.Axis.X ? windowWidth : windowHeight;
    }

    static int journeyMapZoom(int widthBlocks) {
        return Math.ceilDiv(MIN_WORLD_PIXELS * JOURNEYMAP_REGION_BLOCKS, widthBlocks);
    }

    static int ftbChunksZoom(int widthBlocks) {
        return Math.ceilDiv(MIN_WORLD_PIXELS * FTBCHUNKS_ZOOM_BLOCKS, widthBlocks);
    }

    static double xaeroScale(int widthBlocks, double scaleMultiplier) {
        return MIN_WORLD_PIXELS / (widthBlocks * scaleMultiplier);
    }

    private FullscreenZoomFloor() {
    }
}
