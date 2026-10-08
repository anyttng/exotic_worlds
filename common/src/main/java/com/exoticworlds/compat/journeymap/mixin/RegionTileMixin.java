package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Rectangle2D;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.journeymap.JourneyMapFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "journeymap.client.render.map.RegionTile", remap = false)
public abstract class RegionTileMixin {
    @WrapOperation(method = "setPosition",
            at = @At(value = "INVOKE", target = "Ljava/awt/geom/Rectangle2D$Double;contains(DD)Z"))
    private boolean toroidal$showTilesWhoseCopyMeetsTheGrid(Rectangle2D.Double regionBounds, double regionX,
            double regionZ, Operation<Boolean> original) {
        return original.call(regionBounds, regionX, regionZ)
                || JourneyMapFold.active() && JourneyMapFold.regionInView(regionBounds, (int) regionX, (int) regionZ);
    }
}
