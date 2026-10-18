package com.exoticworlds.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.journeymap.SingleCopyOverlay;

@Mixin(targets = "journeymap.api.v2.client.display.Displayable", remap = false)
public abstract class DisplayableMixin implements SingleCopyOverlay {
    @Unique
    private boolean exotic_worlds$drawsOnce;

    @Override
    public void exotic_worlds$drawOnce() {
        exotic_worlds$drawsOnce = true;
    }

    @Override
    public boolean exotic_worlds$drawsOnce() {
        return exotic_worlds$drawsOnce;
    }
}
