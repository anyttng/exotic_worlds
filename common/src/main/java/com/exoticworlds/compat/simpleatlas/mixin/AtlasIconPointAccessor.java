package com.exoticworlds.compat.simpleatlas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "rubbertoe.simple_atlas.client.screen.icon.AtlasIcon$WorldPoint")
public interface AtlasIconPointAccessor {
    @Accessor("x")
    double exotic_worlds$getX();

    @Accessor("z")
    double exotic_worlds$getZ();
}
