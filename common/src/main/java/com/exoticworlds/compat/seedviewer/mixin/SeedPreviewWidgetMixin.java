package com.exoticworlds.compat.seedviewer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.seedviewer.CreationShape;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.acenia.seedviewer.client.screen.createworld.SeedPreviewWidget;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

@Mixin(SeedPreviewWidget.class)
public class SeedPreviewWidgetMixin {
    @ModifyReturnValue(method = "settingsFingerprint", at = @At("RETURN"))
    private String toroidal$fingerprintTheChosenShape(String fingerprint, @Local WorldCreationContext settings) {
        return fingerprint + CreationShape.fingerprint(settings);
    }
}
