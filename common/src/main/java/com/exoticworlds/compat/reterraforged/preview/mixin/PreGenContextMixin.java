package com.exoticworlds.compat.reterraforged.preview.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.preview.TerraForgedPreview;

import net.minecraft.core.Direction;

@Mixin(targets = {"raccoonman.reterraforged.client.gui.screen.presetconfig.Preview2D$PreGenContext",
        "raccoonman.reterraforged.client.gui.screen.presetconfig.Preview3D$PreGenContext"}, remap = false)
public abstract class PreGenContextMixin {
    @ModifyVariable(method = "<init>", at = @At("LOAD"), argsOnly = true, ordinal = 0)
    private int exotic_worlds$holdCenterX(int centerX, @Local(argsOnly = true, ordinal = 2) int zoom) {
        return TerraForgedPreview.heldCenter(Direction.Axis.X, centerX, zoom);
    }

    @ModifyVariable(method = "<init>", at = @At("LOAD"), argsOnly = true, ordinal = 1)
    private int exotic_worlds$holdCenterZ(int centerZ, @Local(argsOnly = true, ordinal = 2) int zoom) {
        return TerraForgedPreview.heldCenter(Direction.Axis.Z, centerZ, zoom);
    }
}
