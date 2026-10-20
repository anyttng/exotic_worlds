package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.nanaky.ultimate_map_atlases.client.AbstractAtlasWidget;

@Mixin(AbstractAtlasWidget.class)
public interface AbstractAtlasWidgetAccessor {
    @Accessor("mapBlocksSize")
    int exotic_worlds$mapBlocksSize();

    @Accessor("currentXCenter")
    double exotic_worlds$currentXCenter();

    @Accessor("currentXCenter")
    void exotic_worlds$setCurrentXCenter(double center);

    @Accessor("currentZCenter")
    double exotic_worlds$currentZCenter();

    @Accessor("currentZCenter")
    void exotic_worlds$setCurrentZCenter(double center);

    @Accessor("zoomLevel")
    float exotic_worlds$zoomLevel();

    @Accessor("zoomLevel")
    void exotic_worlds$setZoomLevel(float zoom);
}
