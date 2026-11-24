package com.exoticworlds.compat.map;

import java.util.function.ToDoubleFunction;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class FullscreenZoomFloor {
    public static final int MIN_WORLD_PIXELS = 64;

    public static final int JOURNEYMAP_REGION_BLOCKS = 512;
    private static final int FTBCHUNKS_ZOOM_BLOCKS = 256;

    public static final int JOURNEYMAP_MAX_ZOOM = 16_384;

    public static final int FTBCHUNKS_MAX_ZOOM = 1_024;

    public static int journeyMapZoom(WorldFold fold) {
        return (int) maxOverLoopedAxes(fold, axis -> journeyMapZoom(fold.getWidthBlocks(axis)));
    }

    public static double xaeroScale(WorldFold fold, double scaleMultiplier) {
        return maxOverLoopedAxes(fold, axis -> xaeroScale(fold.getWidthBlocks(axis), scaleMultiplier));
    }

    public static int ftbChunksZoom(WorldFold fold) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.isLooped(axis)) {
                floor = Math.max(floor, ftbChunksZoom(fold.getWidthBlocks(axis)));
            }
        }

        return floor;
    }

    public static int journeyMapCoverZoom(WorldFold fold, int windowWidthPixels, int windowHeightPixels) {
        return (int) maxOverLoopedAxes(fold,
                axis -> journeyMapCoverZoom(fold.getWidthBlocks(axis), windowSidePixels(axis, windowWidthPixels, windowHeightPixels)));
    }

    public static int ftbChunksCoverZoom(WorldFold fold, int windowWidthPixels, int windowHeightPixels) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.isLooped(axis)) {
                floor = Math.max(floor, ftbChunksCoverZoom(fold.getWidthBlocks(axis), windowSidePixels(axis, windowWidthPixels, windowHeightPixels)));
            }
        }

        return floor;
    }

    public static double xaeroCoverScale(WorldFold fold, double scaleMultiplier, int windowWidthPixels, int windowHeightPixels) {
        return maxOverLoopedAxes(fold,
                axis -> xaeroCoverScale(fold.getWidthBlocks(axis), scaleMultiplier, windowSidePixels(axis, windowWidthPixels, windowHeightPixels)));
    }

    // mc/26.2: Simple Atlas holds its SINGLE view at this zoom.
    public static float simpleAtlasCoverZoom(WorldFold fold, float viewWidthPixels, float viewHeightPixels, int blocksPerTile,
            float tilePixels) {
        return (float) maxOverLoopedAxes(fold, axis -> simpleAtlasCoverZoom(fold.getWidthBlocks(axis),
                windowSidePixels(axis, viewWidthPixels, viewHeightPixels), blocksPerTile, tilePixels));
    }

    public static float ultimateMapAtlasesZoom(WorldFold fold, float viewWidthPixels, int mapBlocks) {
        return (float) minOverLoopedAxes(fold,
                axis -> ultimateMapAtlasesZoom(fold.getWidthBlocks(axis), viewWidthPixels, mapBlocks));
    }

    public static float ultimateMapAtlasesCoverZoom(WorldFold fold, float viewWidthPixels, float viewHeightPixels,
            int mapBlocks) {
        return (float) minOverLoopedAxes(fold, axis -> ultimateMapAtlasesCoverZoom(fold.getWidthBlocks(axis), viewWidthPixels,
                windowSidePixels(axis, viewWidthPixels, viewHeightPixels), mapBlocks));
    }

    public static float ultraTerraForgedZoom(WorldFold fold, int widgetPixels, int previewPixels) {
        return (float) minOverLoopedAxes(fold,
                axis -> ultraTerraForgedZoom(fold.getWidthBlocks(axis), widgetPixels, previewPixels));
    }

    public static float ultraTerraForgedCoverZoom(WorldFold fold, int previewPixels) {
        return (float) minOverLoopedAxes(fold,
                axis -> ultraTerraForgedCoverZoom(fold.getWidthBlocks(axis), previewPixels));
    }

    private static double maxOverLoopedAxes(WorldFold fold, ToDoubleFunction<Direction.Axis> floorOf) {
        double floor = 0.0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.isLooped(axis)) {
                floor = Math.max(floor, floorOf.applyAsDouble(axis));
            }
        }

        return floor;
    }

    private static double minOverLoopedAxes(WorldFold fold, ToDoubleFunction<Direction.Axis> ceilingOf) {
        double ceiling = Double.MAX_VALUE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.isLooped(axis)) {
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

    static float ultimateMapAtlasesZoom(int widthBlocks, float viewWidthPixels, int mapBlocks) {
        return widthBlocks * viewWidthPixels / (MIN_WORLD_PIXELS * mapBlocks);
    }

    static float ultimateMapAtlasesCoverZoom(int widthBlocks, float viewWidthPixels, float sidePixels, int mapBlocks) {
        return widthBlocks * viewWidthPixels / (sidePixels * mapBlocks);
    }

    static double ultraTerraForgedZoom(int widthBlocks, int widgetPixels, int previewPixels) {
        return (double) widthBlocks * widgetPixels / ((double) previewPixels * MIN_WORLD_PIXELS);
    }

    static double ultraTerraForgedCoverZoom(int widthBlocks, int previewPixels) {
        return (double) widthBlocks / previewPixels;
    }

    private static int windowSidePixels(Direction.Axis axis, int windowWidthPixels, int windowHeightPixels) {
        return axis == Direction.Axis.X ? windowWidthPixels : windowHeightPixels;
    }

    private static float windowSidePixels(Direction.Axis axis, float windowWidthPixels, float windowHeightPixels) {
        return axis == Direction.Axis.X ? windowWidthPixels : windowHeightPixels;
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
