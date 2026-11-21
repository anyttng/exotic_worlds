package com.exoticworlds.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.gen.SpawnSearch;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.NoiseSpawnFinder;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;

@Mixin(NoiseSpawnFinder.class)
public class NoiseSpawnFinderMixin {
    @Unique
    private static final String GET_SPAWN_POSITION_AND_FITNESS =
            "Lnet/minecraft/world/level/levelgen/NoiseSpawnFinder;getSpawnPositionAndFitness("
                    + "Lnet/minecraft/world/level/levelgen/densityfunction/DensitySamplerSet;Ljava/util/List;II)"
                    + "Lnet/minecraft/world/level/levelgen/NoiseSpawnFinder$Result;";

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 2)
    private int exotic_worlds$startAtTheSpawnPointX(int blockX) {
        return SpawnSearch.bound().blockX();
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS), index = 3)
    private int exotic_worlds$startAtTheSpawnPointZ(int blockZ) {
        return SpawnSearch.bound().blockZ();
    }

    @WrapOperation(method = "radialSearch", at = @At(value = "INVOKE", target = GET_SPAWN_POSITION_AND_FITNESS))
    private NoiseSpawnFinder.Result exotic_worlds$walkOverTheSurface(DensitySamplerSet samplers,
            List<SpawnTargetPoint> targetPoints, int blockX, int blockZ, Operation<NoiseSpawnFinder.Result> original,
            @Local BlockPos searchOrigin) {
        BlockPos walked = SpawnSearch.walked(searchOrigin, blockX, blockZ);
        return original.call(samplers, targetPoints, walked.getX(), walked.getZ());
    }

    @ModifyArg(method = "getSpawnPositionAndFitness",
            at = @At(value = "INVOKE", target = InjectionTargets.MTH_SQUARE_LONG, ordinal = 0))
    private static long exotic_worlds$distanceFromTheSpawnPointX(long distanceX,
            @Local(argsOnly = true, ordinal = 0) int blockX, @Local(argsOnly = true, ordinal = 1) int blockZ) {
        return SpawnSearch.distanceX(blockX, blockZ);
    }

    @ModifyArg(method = "getSpawnPositionAndFitness",
            at = @At(value = "INVOKE", target = InjectionTargets.MTH_SQUARE_LONG, ordinal = 1))
    private static long exotic_worlds$distanceFromTheSpawnPointZ(long distanceZ,
            @Local(argsOnly = true, ordinal = 0) int blockX, @Local(argsOnly = true, ordinal = 1) int blockZ) {
        return SpawnSearch.distanceZ(blockX, blockZ);
    }
}
