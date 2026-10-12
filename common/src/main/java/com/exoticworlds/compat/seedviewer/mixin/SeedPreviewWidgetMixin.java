package com.exoticworlds.compat.seedviewer.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.CreationShape;
import com.exoticworlds.compat.seedviewer.SeamLines;
import com.exoticworlds.compat.seedviewer.SeedViewerInjectionTargets;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.acenia.seedviewer.client.map.MapCamera;
import net.acenia.seedviewer.client.map.MapViewport;
import net.acenia.seedviewer.client.screen.createworld.SeedPreviewWidget;
import net.acenia.seedviewer.client.screen.createworld.WorldCreationPreviewContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

@Mixin(SeedPreviewWidget.class)
public class SeedPreviewWidgetMixin {
    @Shadow
    @Final
    private MapCamera camera;

    @Shadow
    private MapViewport viewport;

    @Shadow
    private WorldCreationPreviewContext previewContext;

    @ModifyReturnValue(method = "settingsFingerprint", at = @At("RETURN"))
    private String toroidal$fingerprintTheChosenShape(String fingerprint, @Local WorldCreationContext settings) {
        return fingerprint + CreationShape.fingerprint(settings);
    }

    @Inject(method = SeedViewerInjectionTargets.DRAW_TILES,
            at = @At(value = "INVOKE", target = SeedViewerInjectionTargets.DISABLE_SCISSOR))
    private void toroidal$drawTheSeams(GuiGraphics graphics, CallbackInfo callback) {
        SeamLines.draw(graphics,
                ((WorldCreationPreviewContextAccessor) (Object) this.previewContext).toroidal$biomeSampler(),
                this.camera, this.viewport);
    }
}
