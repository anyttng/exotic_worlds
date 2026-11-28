package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.world.generator.TerrainGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.betterend.LapTerrain;

@Mixin(TerrainGenerator.class)
public class TerrainGeneratorMixin {
    @Inject(method = "isLand", at = @At("HEAD"), cancellable = true)
    private static void exotic_worlds$landOnLap(int x, int z, int maxHeight, CallbackInfoReturnable<Boolean> cir) {
        Boolean land = LapTerrain.isLand(x, z, maxHeight);
        if (land != null) {
            cir.setReturnValue(land);
        }
    }
}
