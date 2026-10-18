package com.exoticworlds.compat.terrablender.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.terrablender.LayeredArea;
import com.exoticworlds.compat.terrablender.RegionLayerStack;

import terrablender.worldgen.noise.Area;
import terrablender.worldgen.noise.AreaContext;
import terrablender.worldgen.noise.PixelTransformer;

@Mixin(value = AreaContext.class, remap = false)
public class AreaContextMixin {
    @Unique
    private static final String TRANSFORMER = "Lterrablender/worldgen/noise/PixelTransformer;";
    @Unique
    private static final String AREA = "Lterrablender/worldgen/noise/Area;";

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + ")" + AREA, at = @At("RETURN"))
    private Area exotic_worlds$registerInitialLayer(Area area) {
        ((LayeredArea) (Object) area).exotic_worlds$register(new RegionLayerStack(), 0);
        return area;
    }

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + AREA + ")" + AREA, at = @At("RETURN"))
    private Area exotic_worlds$registerZoomLayer(Area area, PixelTransformer transformer, Area parent) {
        exotic_worlds$registerAbove(area, (LayeredArea) (Object) parent);
        return area;
    }

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + AREA + AREA + ")" + AREA, at = @At("RETURN"))
    private Area exotic_worlds$registerMergeLayer(Area area, PixelTransformer transformer, Area first, Area second) {
        LayeredArea firstLayer = (LayeredArea) (Object) first;
        LayeredArea secondLayer = (LayeredArea) (Object) second;
        exotic_worlds$registerAbove(area, firstLayer.exotic_worlds$depth() >= secondLayer.exotic_worlds$depth() ? firstLayer : secondLayer);
        return area;
    }

    @Unique
    private static void exotic_worlds$registerAbove(Area area, LayeredArea parent) {
        RegionLayerStack stack = parent.exotic_worlds$stack();
        ((LayeredArea) (Object) area).exotic_worlds$register(stack != null ? stack : new RegionLayerStack(),
                parent.exotic_worlds$depth() + 1);
    }
}
