package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.gen.SpawnSearch;

@Mixin(targets = "net.minecraft.world.level.biome.Climate$SpawnFinder")
public class ClimateSpawnFinderMixin {
    private static final String GET_SPAWN_POSITION_AND_FITNESS =
            "Lnet/minecraft/world/level/biome/Climate$SpawnFinder;getSpawnPositionAndFitness("
                    + "Ljava/util/List;Lnet/minecraft/world/level/biome/Climate$Sampler;II)"
                    + "Lnet/minecraft/world/level/biome/Climate$SpawnFinder$Result;";

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 2)
    private int exotic_worlds$startAtTheSpawnPointX(int blockX) {
        return SpawnSearch.bound().blockX();
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 3)
    private int exotic_worlds$startAtTheSpawnPointZ(int blockZ) {
        return SpawnSearch.bound().blockZ();
    }

    @ModifyArg(method = "getSpawnPositionAndFitness",
            at = @At(value = "INVOKE", target = InjectionTargets.MTH_SQUARE_LONG, ordinal = 0))
    private static long exotic_worlds$distanceFromTheSpawnPointX(long blockX) {
        return blockX - SpawnSearch.bound().blockX();
    }

    @ModifyArg(method = "getSpawnPositionAndFitness",
            at = @At(value = "INVOKE", target = InjectionTargets.MTH_SQUARE_LONG, ordinal = 1))
    private static long exotic_worlds$distanceFromTheSpawnPointZ(long blockZ) {
        return blockZ - SpawnSearch.bound().blockZ();
    }
}
