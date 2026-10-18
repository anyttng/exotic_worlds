package com.exoticworlds.compat.simpleatlas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "rubbertoe.simple_atlas.client.screen.AtlasScreen$AtlasViewport")
public interface AtlasViewportAccessor {
    @Accessor("contentX")
    float exotic_worlds$contentX();

    @Accessor("contentY")
    float exotic_worlds$contentY();

    @Accessor("contentWidth")
    float exotic_worlds$contentWidth();

    @Accessor("contentHeight")
    float exotic_worlds$contentHeight();
}
