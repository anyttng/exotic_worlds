package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.exoticworlds.engine.seam.SeamRange;
import com.hollingsworth.arsnouveau.common.entity.goal.DistanceRestrictedGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = DistanceRestrictedGoal.class, remap = false)
public abstract class DistanceRestrictedGoalMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @WrapOperation(method = "isInRange", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/util/BlockUtil;distanceFrom(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)D"))
    private double exotic_worlds$rangeThroughSeam(BlockPos start, BlockPos end, Operation<Double> original) {
        if (start == null || end == null) {
            return original.call(start, end);
        }

        return Math.sqrt(SeamRange.sqr(this.exotic_worlds$bound.wrapped(), start, end));
    }
}
