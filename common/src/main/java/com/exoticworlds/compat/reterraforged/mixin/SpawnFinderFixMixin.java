package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.engine.gen.SpawnSearch;

import raccoonman.reterraforged.world.worldgen.cell.biome.spawn.SpawnFinderFix;

@Mixin(SpawnFinderFix.class)
public class SpawnFinderFixMixin {
    private static final String GET_SPAWN_POSITION_AND_FITNESS =
            "Lraccoonman/reterraforged/world/worldgen/cell/biome/spawn/SpawnFinderFix;getSpawnPositionAndFitness("
                    + "Ljava/util/List;Lnet/minecraft/world/level/biome/Climate$Sampler;II)"
                    + "Lraccoonman/reterraforged/world/worldgen/cell/biome/spawn/SpawnFinderFix$Result;";

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 2)
    private int exotic_worlds$startAtTheSpawnPointX(int centerX) {
        SpawnPoint point = SpawnSearch.bound();
        return point.equals(SpawnPoint.ZERO) ? centerX : point.blockX();
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 3)
    private int exotic_worlds$startAtTheSpawnPointZ(int centerZ) {
        SpawnPoint point = SpawnSearch.bound();
        return point.equals(SpawnPoint.ZERO) ? centerZ : point.blockZ();
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
