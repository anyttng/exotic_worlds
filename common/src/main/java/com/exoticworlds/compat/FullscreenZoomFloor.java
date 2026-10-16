package com.exoticworlds.compat;

import java.util.function.ToDoubleFunction;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.CoordinateConstants;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class FullscreenZoomFloor {
    public static final int MIN_WORLD_PIXELS = 64;

    public static final int JOURNEYMAP_REGION_BLOCKS = 512;

    public static final int JOURNEYMAP_MAX_ZOOM = 16_384;

    public static int journeyMapZoom(ToroidalShape shape) {
        return (int) maxOverLoopedAxes(shape, axis -> journeyMapZoom(shape.widthBlocks(axis)));
    }

    public static double xaeroScale(ToroidalShape shape, double scaleMultiplier) {
        return maxOverLoopedAxes(shape, axis -> xaeroScale(shape.widthBlocks(axis), scaleMultiplier));
    }

    public static int journeyMapCoverZoom(ToroidalShape shape, int windowWidth, int windowHeight) {
        return (int) maxOverLoopedAxes(shape,
                axis -> journeyMapCoverZoom(shape.widthBlocks(axis), windowSide(axis, windowWidth, windowHeight)));
    }

    public static double xaeroCoverScale(ToroidalShape shape, double scaleMultiplier, int windowWidth, int windowHeight) {
        return maxOverLoopedAxes(shape,
                axis -> xaeroCoverScale(shape.widthBlocks(axis), scaleMultiplier, windowSide(axis, windowWidth, windowHeight)));
    }

    // mc/26.2: Simple Atlas holds its SINGLE view at this zoom.
    public static float simpleAtlasCoverZoom(ToroidalShape shape, float viewWidth, float viewHeight, int blocksPerTile,
            float tilePixels) {
        return (float) maxOverLoopedAxes(shape, axis -> simpleAtlasCoverZoom(shape.widthBlocks(axis),
                windowSide(axis, viewWidth, viewHeight), blocksPerTile, tilePixels));
    }

    private static double maxOverLoopedAxes(ToroidalShape shape, ToDoubleFunction<Direction.Axis> floorOf) {
        double floor = 0.0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, floorOf.applyAsDouble(axis));
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

    static double xaeroCoverScale(int widthBlocks, double scaleMultiplier, int windowPixels) {
        return windowPixels / (widthBlocks * scaleMultiplier);
    }

    static float simpleAtlasCoverZoom(int widthBlocks, float viewPixels, int blocksPerTile, float tilePixels) {
        return viewPixels * blocksPerTile / (tilePixels * widthBlocks);
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

    static double xaeroScale(int widthBlocks, double scaleMultiplier) {
        return MIN_WORLD_PIXELS / (widthBlocks * scaleMultiplier);
    }

    private FullscreenZoomFloor() {
    }
}
