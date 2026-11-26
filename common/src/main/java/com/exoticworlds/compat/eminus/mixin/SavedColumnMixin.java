package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.ingest.BiomeWindow;
import com.eminus.ingest.saved.SavedChunk;
import com.eminus.ingest.saved.SavedColumn;
import com.exoticworlds.accessors.BiomeZoomOwner;
import com.exoticworlds.compat.eminus.SavedChunkOwner;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(SavedColumn.class)
public class SavedColumnMixin {
    @Shadow
    @Final
    private SavedChunk center;

    @ModifyReturnValue(method = "biomeWindow", at = @At("RETURN"))
    private BiomeWindow exotic_worlds$zoomOnTheSavedLevel(BiomeWindow window) {
        ((BiomeZoomOwner) ((BiomeWindowAccessor) (Object) window).exotic_worlds$getManager())
                .exotic_worlds$setOwner(SavedChunkOwner.getOwner(center));
        return window;
    }
}
