package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.ingest.BiomeWindow;
import com.exoticworlds.accessors.BiomeZoomOwner;
import com.exoticworlds.compat.eminus.EminusImports;
import com.exoticworlds.compat.eminus.SavedChunkOwner;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Pseudo
@Mixin(targets = "com.eminus.ingest.saved.SavedColumn", remap = false)
public class SavedColumnMixin {
    @ModifyReturnValue(method = "biomeWindow", at = @At("RETURN"))
    private BiomeWindow exotic_worlds$zoomOnTheSavedLevel(BiomeWindow window) {
        ((BiomeZoomOwner) ((BiomeWindowAccessor) (Object) window).exotic_worlds$manager())
                .exotic_worlds$setOwner(SavedChunkOwner.ownerOf(EminusImports.center(this)));
        return window;
    }
}
