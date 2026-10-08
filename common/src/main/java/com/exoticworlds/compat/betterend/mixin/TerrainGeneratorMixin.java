package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.world.generator.TerrainGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.compat.betterend.EndTerrainLap;
import com.exoticworlds.compat.betterend.LapTerrain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

@Mixin(TerrainGenerator.class)
public class TerrainGeneratorMixin {
    @Inject(method = "onServerLevelInit", at = @At("RETURN"))
    private static void toroidal$captureLap(ServerLevel level, LevelStem levelStem, long seed, CallbackInfo ci) {
        if (level.dimension() == Level.END) {
            EndTerrainLap.capture(level, seed);
        }
    }

    @Inject(method = "fillTerrainDensity", at = @At("HEAD"), cancellable = true)
    private static void toroidal$fillOnLap(double[] buffer, int posX, int posZ, int scaleXZ, int scaleY,
            int maxHeight, CallbackInfo ci) {
        if (LapTerrain.fill(buffer, posX, posZ, scaleXZ, scaleY, maxHeight)) {
            ci.cancel();
        }
    }

    @Inject(method = "isLand", at = @At("HEAD"), cancellable = true)
    private static void toroidal$landOnLap(int x, int z, int maxHeight, CallbackInfoReturnable<Boolean> cir) {
        Boolean land = LapTerrain.isLand(x, z, maxHeight);
        if (land != null) {
            cir.setReturnValue(land);
        }
    }
}
