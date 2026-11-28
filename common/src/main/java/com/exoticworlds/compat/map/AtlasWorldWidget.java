package com.exoticworlds.compat.map;

import net.minecraft.world.phys.Vec3;

public interface AtlasWorldWidget {
    int exotic_worlds$getMapBlocks();

    int exotic_worlds$getWidthPixels();

    int exotic_worlds$getHeightPixels();

    float exotic_worlds$getZoom();

    void exotic_worlds$setZoom(float zoom);

    float exotic_worlds$getTargetZoom();

    void exotic_worlds$setTargetZoom(float zoom);

    void exotic_worlds$setZoomSteps(float steps);

    Vec3 exotic_worlds$getCenter();

    void exotic_worlds$setCenter(Vec3 center);

    Vec3 exotic_worlds$getTargetCenter();

    void exotic_worlds$setTargetCenter(Vec3 center);
}
