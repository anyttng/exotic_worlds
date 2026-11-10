package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.engine.gen.SpawnSearch;

import net.minecraft.world.level.levelgen.NoiseSpawnFinder;

@Mixin(NoiseSpawnFinder.class)
public class NoiseSpawnFinderMixin {
    private static final String GET_SPAWN_POSITION_AND_FITNESS =
            "Lnet/minecraft/world/level/levelgen/NoiseSpawnFinder;getSpawnPositionAndFitness("
                    + "Lnet/minecraft/world/level/levelgen/densityfunction/DensitySamplerSet;Ljava/util/List;II)"
                    + "Lnet/minecraft/world/level/levelgen/NoiseSpawnFinder$Result;";
    private static final String SQUARE = "Lnet/minecraft/util/Mth;square(J)J";

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 2)
    private int exotic_worlds$startAtTheSpawnPointX(int blockX) {
        return SpawnSearch.bound().blockX();
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 3)
    private int exotic_worlds$startAtTheSpawnPointZ(int blockZ) {
        return SpawnSearch.bound().blockZ();
    }

    @ModifyArg(method = "getSpawnPositionAndFitness", at = @At(value = "INVOKE", target = SQUARE, ordinal = 0))
    private static long exotic_worlds$distanceFromTheSpawnPointX(long blockX) {
        return blockX - SpawnSearch.bound().blockX();
    }

    @ModifyArg(method = "getSpawnPositionAndFitness", at = @At(value = "INVOKE", target = SQUARE, ordinal = 1))
    private static long exotic_worlds$distanceFromTheSpawnPointZ(long blockZ) {
        return blockZ - SpawnSearch.bound().blockZ();
    }
}
