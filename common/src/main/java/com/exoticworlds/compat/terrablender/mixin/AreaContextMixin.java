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

@Mixin(AreaContext.class)
public class AreaContextMixin {
    @Unique
    private static final String TRANSFORMER = "Lterrablender/worldgen/noise/PixelTransformer;";
    @Unique
    private static final String AREA = "Lterrablender/worldgen/noise/Area;";

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + ")" + AREA, at = @At("RETURN"))
    private Area toroidal$registerInitialLayer(Area area) {
        ((LayeredArea) (Object) area).toroidal$register(new RegionLayerStack(), 0);
        return area;
    }

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + AREA + ")" + AREA, at = @At("RETURN"))
    private Area toroidal$registerZoomLayer(Area area, PixelTransformer transformer, Area parent) {
        toroidal$registerAbove(area, (LayeredArea) (Object) parent);
        return area;
    }

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + AREA + AREA + ")" + AREA, at = @At("RETURN"))
    private Area toroidal$registerMergeLayer(Area area, PixelTransformer transformer, Area first, Area second) {
        LayeredArea firstLayer = (LayeredArea) (Object) first;
        LayeredArea secondLayer = (LayeredArea) (Object) second;
        toroidal$registerAbove(area, firstLayer.toroidal$depth() >= secondLayer.toroidal$depth() ? firstLayer : secondLayer);
        return area;
    }

    @Unique
    private static void toroidal$registerAbove(Area area, LayeredArea parent) {
        RegionLayerStack stack = parent.toroidal$stack();
        ((LayeredArea) (Object) area).toroidal$register(stack != null ? stack : new RegionLayerStack(),
                parent.toroidal$depth() + 1);
    }
}
