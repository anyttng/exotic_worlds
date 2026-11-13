package com.exoticworlds.compat.freeterraforged.preview.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.preview.PreviewHandle;
import com.exoticworlds.compat.preview.TerraForgedPreview;

import etcodehome.freeterraforged.client.gui.screen.presetconfig.Preview2D;
import etcodehome.freeterraforged.client.gui.screen.presetconfig.Preview3D;

@Mixin(value = {Preview2D.class, Preview3D.class}, remap = false)
public abstract class PreviewMapMixin {
    @ModifyReturnValue(method = "getZoom", at = @At("RETURN"))
    private int exotic_worlds$capTheZoom(int zoom) {
        return TerraForgedPreview.cappedZoom((PreviewHandle) this, zoom);
    }

    @Inject(method = "updateLegend", at = @At("RETURN"))
    private void exotic_worlds$readInsideTheWorld(int mouseX, int mouseY, CallbackInfoReturnable<Boolean> callback) {
        TerraForgedPreview.foldHovered((PreviewHandle) this);
    }
}
