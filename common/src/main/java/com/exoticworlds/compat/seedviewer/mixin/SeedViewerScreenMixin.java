package com.exoticworlds.compat.seedviewer.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.seedviewer.SeamLines;
import com.exoticworlds.compat.seedviewer.SeedViewerInjectionTargets;

import net.acenia.seedviewer.client.map.MapCamera;
import net.acenia.seedviewer.client.map.MapViewport;
import net.acenia.seedviewer.client.screen.SeedViewerScreen;
import net.acenia.seedviewer.client.worldgen.BiomeSampler;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(SeedViewerScreen.class)
public class SeedViewerScreenMixin {
    @Shadow
    @Final
    private MapCamera camera;

    @Shadow
    private MapViewport viewport;

    @Shadow
    @Final
    private @Nullable BiomeSampler biomeSampler;

    @Inject(method = SeedViewerInjectionTargets.DRAW_TILES,
            at = @At(value = "INVOKE", target = SeedViewerInjectionTargets.DISABLE_SCISSOR))
    private void toroidal$drawTheSeams(GuiGraphics graphics, CallbackInfo callback) {
        if (this.biomeSampler != null) {
            SeamLines.draw(graphics, this.biomeSampler, this.camera, this.viewport);
        }
    }
}
