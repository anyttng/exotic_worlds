package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.entity.ai.FollowWalkTargetTask;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.PathfinderMob;

@Mixin(FollowWalkTargetTask.class)
public class FollowWalkTargetMixin {
    @WrapOperation(
            method = "hasReached",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_MANHATTAN))
    private int exotic_worlds$arrivalDistanceThroughSeam(BlockPos targetPos, Vec3i bodyPos, Operation<Integer> original,
            @Local(argsOnly = true) PathfinderMob body) {
        return SeamRange.manhattan(body, targetPos, bodyPos);
    }

    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double exotic_worlds$retargetDistanceThroughSeam(BlockPos targetPos, Vec3i lastTargetPos,
            Operation<Double> original, @Local(argsOnly = true) PathfinderMob body) {
        return SeamRange.sqr(body, targetPos, lastTargetPos);
    }
}
