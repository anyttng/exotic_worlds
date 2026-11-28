package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.utils.DecorationHolder;

@Mixin(DecorationBookmarkButton.class)
public interface DecorationBookmarkButtonAccessor {
    @Accessor("holder")
    DecorationHolder exotic_worlds$getHolder();
}
