package com.exoticworlds.compat.freeterraforged.preview.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.freeterraforged.preview.FreeTerraForgedPreview;

import net.minecraft.core.Direction;

@Mixin(targets = "etcodehome.freeterraforged.client.gui.screen.presetconfig.IPreviewHandler$PreGenContext",
        remap = false)
public abstract class PreGenContextMixin {
    @ModifyVariable(method = "<init>", at = @At("LOAD"), argsOnly = true, ordinal = 0)
    private int exotic_worlds$holdCenterX(int centerX, @Local(argsOnly = true, ordinal = 2) int zoom) {
        return FreeTerraForgedPreview.heldCenter(Direction.Axis.X, centerX, zoom);
    }

    @ModifyVariable(method = "<init>", at = @At("LOAD"), argsOnly = true, ordinal = 1)
    private int exotic_worlds$holdCenterZ(int centerZ, @Local(argsOnly = true, ordinal = 2) int zoom) {
        return FreeTerraForgedPreview.heldCenter(Direction.Axis.Z, centerZ, zoom);
    }
}
