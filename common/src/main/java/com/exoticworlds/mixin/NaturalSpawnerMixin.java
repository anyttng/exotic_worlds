package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @WrapOperation(
            method = "isRightDistanceToPlayerAndSpawnPoint",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private static boolean exotic_worlds$spawnRadiusThroughSeam(BlockPos spawnPos, Position candidate, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) ServerLevel level) {
        return original.call(spawnPos, SeamRange.towardCenterOf(level, spawnPos, candidate), distance);
    }

    @ModifyExpressionValue(
            method = "createState",
            at = @At(value = "NEW", target = "net/minecraft/world/level/PotentialCalculator"))
    private static PotentialCalculator exotic_worlds$bindSpawnPotentialToLevel(PotentialCalculator spawnPotential,
            @Local(argsOnly = true) ServerLevel level) {
        ((FoldHolder) spawnPotential).exotic_worlds$setFold(WorldLoopAttachments.foldOf(level));
        return spawnPotential;
    }
}
