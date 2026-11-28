package com.exoticworlds.compat.map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class AtlasWorldView {
    private static final float FIRST_DISCRETE_ZOOM = 2.0F;

    public static void hold(@Nullable WorldFold worldFold, MapCopies copies, AtlasWorldWidget widget, boolean shouldFold) {
        if (worldFold == null) {
            return;
        }

        int mapBlocks = widget.exotic_worlds$getMapBlocks();
        capZoom(widget, zoomCeiling(worldFold, copies, widget.exotic_worlds$getWidthPixels(),
                widget.exotic_worlds$getHeightPixels(), mapBlocks));
        if (copies == MapCopies.SINGLE) {
            holdCenters(widget, worldFold, mapBlocks, shouldFold);
        }
    }

    static float zoomCeiling(@Nullable WorldFold worldFold, MapCopies copies, int widthPixels, int heightPixels,
            int mapBlocks) {
        if (worldFold == null) {
            return Float.MAX_VALUE;
        }

        return copies == MapCopies.SINGLE
                ? FullscreenZoomFloor.atlasCoverZoom(worldFold, widthPixels, heightPixels, mapBlocks)
                : FullscreenZoomFloor.atlasZoom(worldFold, widthPixels, mapBlocks);
    }

    static float discreteZoomSteps(float ceiling) {
        return Math.max(0.0F, 2.0F * (float) Math.floor((ceiling - FIRST_DISCRETE_ZOOM) / 2.0F));
    }

    static Vec3 held(@Nullable WorldFold worldFold, Vec3 center, float zoom, int widthPixels, int heightPixels,
            int mapBlocks, boolean shouldFold) {
        Vec3 seated = shouldFold ? ClientFolds.fold(worldFold, center) : center;
        double halfX = zoom * mapBlocks / 2.0;
        double halfZ = halfX * heightPixels / widthPixels;
        return new Vec3(AxisCopies.of(worldFold, Direction.Axis.X).clampView(seated.x, halfX), 0.0,
                AxisCopies.of(worldFold, Direction.Axis.Z).clampView(seated.z, halfZ));
    }

    private static void capZoom(AtlasWorldWidget widget, float ceiling) {
        if (widget.exotic_worlds$getTargetZoom() > ceiling) {
            widget.exotic_worlds$setTargetZoom(ceiling);
            widget.exotic_worlds$setZoomSteps(discreteZoomSteps(ceiling));
        }

        if (widget.exotic_worlds$getZoom() > ceiling) {
            widget.exotic_worlds$setZoom(ceiling);
        }
    }

    private static void holdCenters(AtlasWorldWidget widget, WorldFold worldFold, int mapBlocks, boolean shouldFold) {
        int widthPixels = widget.exotic_worlds$getWidthPixels();
        int heightPixels = widget.exotic_worlds$getHeightPixels();
        widget.exotic_worlds$setCenter(held(worldFold, widget.exotic_worlds$getCenter(), widget.exotic_worlds$getZoom(),
                widthPixels, heightPixels, mapBlocks, shouldFold));
        widget.exotic_worlds$setTargetCenter(held(worldFold, widget.exotic_worlds$getTargetCenter(),
                widget.exotic_worlds$getTargetZoom(), widthPixels, heightPixels, mapBlocks, shouldFold));
    }

    private AtlasWorldView() {
    }
}
