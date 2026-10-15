package com.exoticworlds.compat.simpleatlas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "rubbertoe.simple_atlas.client.screen.AtlasScreen$AtlasViewport")
public interface AtlasViewportAccessor {
    @Accessor("contentX")
    float toroidal$contentX();

    @Accessor("contentY")
    float toroidal$contentY();

    @Accessor("contentWidth")
    float toroidal$contentWidth();

    @Accessor("contentHeight")
    float toroidal$contentHeight();
}
