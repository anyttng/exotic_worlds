package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.level.pathfinder.Node;

@Mixin(TargetGoal.class)
public class TargetGoalMixin {
    @Shadow
    @Final
    protected Mob mob;

    @WrapOperation(
            method = "canReach",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getBlockX()I"))
    private int toroidal$reachTargetNearX(LivingEntity target, Operation<Integer> original, @Local Node last) {
        return toroidal$targetNear(last, new BlockPos(original.call(target), target.getBlockY(), target.getBlockZ()))
                .getX();
    }

    @WrapOperation(
            method = "canReach",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getBlockZ()I"))
    private int toroidal$reachTargetNearZ(LivingEntity target, Operation<Integer> original, @Local Node last) {
        return toroidal$targetNear(last, new BlockPos(target.getBlockX(), target.getBlockY(), original.call(target)))
                .getZ();
    }

    @Unique
    private BlockPos toroidal$targetNear(Node last, BlockPos target) {
        return WorldLoopAttachments.transformerOf(this.mob.level()).nearestCopy(last.asBlockPos(), target);
    }
}
