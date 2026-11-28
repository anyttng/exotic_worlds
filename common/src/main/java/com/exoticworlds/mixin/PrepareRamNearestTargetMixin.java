package com.exoticworlds.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedOrder;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.PrepareRamNearestTarget;
import net.minecraft.world.phys.Vec3;

@Mixin(PrepareRamNearestTarget.class)
public class PrepareRamNearestTargetMixin {
    @ModifyArg(
            method = "calculateRammingStartPosition",
            at = @At(value = "INVOKE", target = InjectionTargets.STREAM_SORTED),
            index = 0)
    private Comparator<BlockPos> exotic_worlds$ramStartOrderThroughSeam(Comparator<BlockPos> original,
            @Local(argsOnly = true) PathfinderMob body) {
        WorldFold fold = ((FoldSource) body).exotic_worlds$getWrappedFold();
        if (fold == null) {
            return original;
        }

        return FoldedOrder.around(original, fold, body.blockPosition());
    }

    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/PrepareRamNearestTarget$RamCandidate;getStartPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$ramStartThroughSeam(PrepareRamNearestTarget.RamCandidate candidate,
            Operation<BlockPos> original, @Local(argsOnly = true) PathfinderMob body) {
        return WorldLoopAttachments.getFold(body.level())
                .foldFrom(Vec3.atCenterOf(candidate.getTargetPosition()), original.call(candidate));
    }

    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/PrepareRamNearestTarget;getEdgeOfBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$ramEdgeThroughSeam(PrepareRamNearestTarget<?> self, BlockPos startRamPos, BlockPos targetPos,
            Operation<Vec3> original, @Local(argsOnly = true) PathfinderMob body) {
        WorldFold fold = ((FoldSource) body).exotic_worlds$getWrappedFold();
        return original.call(self, NearestCopy.toward(fold, targetPos, startRamPos), targetPos);
    }
}
