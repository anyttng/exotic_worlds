package com.toroidalworld.compat;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class FullscreenZoomFloor {
    public static final int MIN_WORLD_PIXELS = 64;

    public static final int JOURNEYMAP_REGION_BLOCKS = 512;
    private static final int FTBCHUNKS_ZOOM_BLOCKS = 256;

    public static final int JOURNEYMAP_MAX_ZOOM = 16_384;

    public static final int FTBCHUNKS_MAX_ZOOM = 1_024;

    public static int journeyMapZoom(ToroidalShape shape) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, journeyMapZoom(shape.widthBlocks(axis)));
            }
        }

        return floor;
    }

    public static double xaeroScale(ToroidalShape shape, double scaleMultiplier) {
        double floor = 0.0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, xaeroScale(shape.widthBlocks(axis), scaleMultiplier));
            }
        }

        return floor;
    }

    public static int ftbChunksZoom(ToroidalShape shape) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, ftbChunksZoom(shape.widthBlocks(axis)));
            }
        }

        return floor;
    }

    public static int journeyMapCoverZoom(ToroidalShape shape, int windowWidth, int windowHeight) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, journeyMapCoverZoom(shape.widthBlocks(axis), windowSide(axis, windowWidth, windowHeight)));
            }
        }

        return floor;
    }

    public static int ftbChunksCoverZoom(ToroidalShape shape, int windowWidth, int windowHeight) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, ftbChunksCoverZoom(shape.widthBlocks(axis), windowSide(axis, windowWidth, windowHeight)));
            }
        }

        return floor;
    }

    public static double xaeroCoverScale(ToroidalShape shape, double scaleMultiplier, int windowWidth, int windowHeight) {
        double floor = 0.0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor,
                        xaeroCoverScale(shape.widthBlocks(axis), scaleMultiplier, windowSide(axis, windowWidth, windowHeight)));
            }
        }

        return floor;
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

    private static int windowSide(Direction.Axis axis, int windowWidth, int windowHeight) {
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
