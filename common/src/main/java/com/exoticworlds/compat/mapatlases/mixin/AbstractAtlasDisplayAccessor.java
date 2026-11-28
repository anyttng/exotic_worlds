package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import pepjebs.mapatlases.client.AbstractAtlasDisplay;

@Mixin(AbstractAtlasDisplay.class)
public interface AbstractAtlasDisplayAccessor {
    @Accessor("mapBlocksSize")
    int exotic_worlds$getMapSizeBlocks();

    @Accessor("currentXCenter")
    double exotic_worlds$getCurrentXCenter();

    @Accessor("currentXCenter")
    void exotic_worlds$setCurrentXCenter(double center);

    @Accessor("currentZCenter")
    double exotic_worlds$getCurrentZCenter();

    @Accessor("currentZCenter")
    void exotic_worlds$setCurrentZCenter(double center);

    @Accessor("zoomLevel")
    float exotic_worlds$getZoomLevel();

    @Accessor("zoomLevel")
    void exotic_worlds$setZoomLevel(float zoom);
}
