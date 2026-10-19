package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.ingest.BiomeWindow;
import com.eminus.ingest.IngestService;
import com.exoticworlds.accessors.BiomeZoomOwner;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(IngestService.class)
public class IngestServiceBiomeMixin {
    @ModifyReturnValue(method = "biomeWindow", at = @At("RETURN"))
    private static BiomeWindow exotic_worlds$zoomOnTheChunksLevel(BiomeWindow window,
            @Local(argsOnly = true) LevelChunk chunk) {
        ((BiomeZoomOwner) ((BiomeWindowAccessor) (Object) window).exotic_worlds$manager()).exotic_worlds$setOwner(
                chunk.getLevel());
        return window;
    }
}
