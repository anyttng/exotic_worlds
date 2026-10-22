package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(TransportItemsBetweenContainers.class)
public class TransportItemsBetweenContainersMixin {
    @ModifyExpressionValue(
            method = "getTransportTarget",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/ChestBlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$candidateChestThroughSeam(BlockPos chestPos, @Local(argsOnly = true) PathfinderMob body) {
        return SeamSteering.nearestCopy(body, chestPos);
    }

    @ModifyExpressionValue(
            method = "isTargetValidToPick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$searchAreaPosThroughSeam(BlockPos chestPos, @Local(argsOnly = true) PathfinderMob body) {
        return SeamSteering.nearestCopy(body, chestPos);
    }

    @ModifyExpressionValue(
            method = "isWithinTargetDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/AABB;"))
    private AABB exotic_worlds$reachBoxThroughSeam(AABB targetBox, @Local(argsOnly = true) PathfinderMob body,
            @Local(argsOnly = true) Vec3 fromPos) {
        return FoldedBoxQuery.toward(body, fromPos, targetBox);
    }

    @ModifyExpressionValue(
            method = "canSeeAnyTargetSide",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getCenter()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$sightCenterThroughSeam(Vec3 center, @Local(argsOnly = true) PathfinderMob body) {
        return SeamSteering.nearestCopy(body, center);
    }

    @ModifyExpressionValue(
            method = "lambda$canSeeAnyTargetSide$1",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult exotic_worlds$sightHitThroughSeam(BlockHitResult hit,
            @Local(argsOnly = true) PathfinderMob body) {
        BlockPos hitPos = hit.getBlockPos();
        BlockPos wrapped = WorldLoopAttachments.transformerOf(body.level()).fold(hitPos);
        return wrapped == hitPos ? hit : hit.withPosition(wrapped);
    }
}
