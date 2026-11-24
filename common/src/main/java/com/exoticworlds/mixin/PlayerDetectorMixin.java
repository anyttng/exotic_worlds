package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.phys.Vec3;

@Mixin(PlayerDetector.class)
public interface PlayerDetectorMixin {
    @WrapOperation(
            method = {"lambda$static$1", "lambda$static$4"},
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private static boolean exotic_worlds$detectionRangeThroughSeam(BlockPos playerPos, Vec3i spawnerPos, double rangeBlocks,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return original.call(playerPos, SeamRange.toward(player, playerPos, spawnerPos), rangeBlocks);
    }

    @WrapMethod(method = "inLineOfSight")
    private static boolean exotic_worlds$sightLineThroughSeam(Level level, Vec3 origin, Vec3 dest, Operation<Boolean> original) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(level);
        return original.call(level, origin, NearestCopy.toward(fold, origin, dest));
    }
}
