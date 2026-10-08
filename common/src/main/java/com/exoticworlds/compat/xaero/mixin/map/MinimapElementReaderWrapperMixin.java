package com.exoticworlds.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;

import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.map.mods.minimap.element.MinimapElementReaderWrapper;

@Mixin(value = MinimapElementReaderWrapper.class, remap = false)
public abstract class MinimapElementReaderWrapperMixin {
    @Shadow
    @Final
    private MinimapElementReader<Object, Object> reader;

    @ModifyReturnValue(method = "getRenderX", at = @At("RETURN"))
    private double toroidal$foldRenderX(double original, Object element, Object context, float partialTicks) {
        return XaeroWorldMapFold.foldPoint(original, this.reader.getRenderZ(element, context, partialTicks)).x;
    }

    @ModifyReturnValue(method = "getRenderZ", at = @At("RETURN"))
    private double toroidal$foldRenderZ(double original, Object element, Object context, float partialTicks) {
        return XaeroWorldMapFold.foldPoint(this.reader.getRenderX(element, context, partialTicks), original).z;
    }
}
