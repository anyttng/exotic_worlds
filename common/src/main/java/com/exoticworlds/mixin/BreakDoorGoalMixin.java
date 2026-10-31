package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;

@Mixin(BreakDoorGoal.class)
public class BreakDoorGoalMixin {
    @WrapOperation(
            method = "canContinueToUse",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean exotic_worlds$doorRangeThroughSeam(BlockPos doorPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return original.call(doorPos,
                SeamRange.towardCenterOf(((DoorInteractGoalAccessor) this).exotic_worlds$mob(), doorPos, bodyPosition),
                distance);
    }
}
