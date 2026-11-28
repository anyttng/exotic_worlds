package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.MapWidget;

@Mixin(MapWidget.class)
public interface MapWidgetAccessor {
    @Accessor("mapScreen")
    AtlasOverviewScreen exotic_worlds$getMapScreen();
}
