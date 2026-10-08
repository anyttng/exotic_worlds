package com.exoticworlds.compat.terrablender.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.terrablender.LayeredArea;
import com.exoticworlds.compat.terrablender.RegionLayerStack;

import terrablender.worldgen.noise.Area;
import terrablender.worldgen.noise.AreaContext;
import terrablender.worldgen.noise.PixelTransformer;

@Mixin(value = AreaContext.class, remap = false)
public class AreaContextMixin {
    private static final String TRANSFORMER = "Lterrablender/worldgen/noise/PixelTransformer;";
    private static final String AREA = "Lterrablender/worldgen/noise/Area;";

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + ")" + AREA, at = @At("RETURN"))
    private Area toroidal$enrolInitialLayer(Area area) {
        ((LayeredArea) (Object) area).toroidal$enrol(new RegionLayerStack(), 0);
        return area;
    }

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + AREA + ")" + AREA, at = @At("RETURN"))
    private Area toroidal$enrolZoomLayer(Area area, PixelTransformer transformer, Area parent) {
        enrolAbove(area, (LayeredArea) (Object) parent);
        return area;
    }

    @ModifyReturnValue(method = "createResult(" + TRANSFORMER + AREA + AREA + ")" + AREA, at = @At("RETURN"))
    private Area toroidal$enrolMergeLayer(Area area, PixelTransformer transformer, Area first, Area second) {
        LayeredArea firstLayer = (LayeredArea) (Object) first;
        LayeredArea secondLayer = (LayeredArea) (Object) second;
        enrolAbove(area, firstLayer.toroidal$depth() >= secondLayer.toroidal$depth() ? firstLayer : secondLayer);
        return area;
    }

    private static void enrolAbove(Area area, LayeredArea parent) {
        RegionLayerStack stack = parent.toroidal$stack();
        ((LayeredArea) (Object) area).toroidal$enrol(stack != null ? stack : new RegionLayerStack(),
                parent.toroidal$depth() + 1);
    }
}
