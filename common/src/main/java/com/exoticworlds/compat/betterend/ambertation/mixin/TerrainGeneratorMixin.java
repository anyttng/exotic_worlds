package com.exoticworlds.compat.betterend.ambertation.mixin;

import de.ambertation.wover.generator.api.biomesource.end.WoverEndConfig;
import org.betterx.betterend.world.generator.GeneratorOptions;
import org.betterx.betterend.world.generator.TerrainGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.betterend.EndTerrainLap;
import com.exoticworlds.compat.betterend.LapIslandPlacement;
import com.exoticworlds.compat.betterend.LapTerrain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

@Mixin(TerrainGenerator.class)
public class TerrainGeneratorMixin {
    @Inject(method = "onServerLevelInit", at = @At("RETURN"))
    private static void exotic_worlds$captureLap(ServerLevel level, LevelStem levelStem, long seed, CallbackInfo ci) {
        if (level.dimension() == Level.END) {
            WoverEndConfig config = TerrainGeneratorConfigAccessor.exotic_worlds$getConfig();
            EndTerrainLap.capture(level, config == null
                    ? LapIslandPlacement.Center.NONE
                    : new LapIslandPlacement.Center(GeneratorOptions.hasCentralIsland(), config.centerBiomesSize,
                            config.innerVoidRadiusSquared));
        }
    }

    @Inject(method = "computeColumnDensity", at = @At("HEAD"), cancellable = true)
    private static void exotic_worlds$fillOnLap(double[] buffer, int posX, int posZ, int scaleXZ, int scaleY,
            int maxHeight, CallbackInfo ci) {
        if (LapTerrain.tryFill(buffer, posX, posZ, scaleXZ, scaleY, maxHeight)) {
            ci.cancel();
        }
    }
}
