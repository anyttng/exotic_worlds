package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuild;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = LinearBuild.class, remap = false)
public abstract class LinearBuildMixin {
    @WrapOperation(method = "isWithinAxisRange", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$casterBlockTowardTarget(LivingEntity entity, Operation<BlockPos> original,
            @Local(argsOnly = true) BlockPos target) {
        return SeamRange.toward(entity, target, original.call(entity));
    }

    @ModifyVariable(method = "collectLinePositions", at = @At("STORE"), ordinal = 0)
    private BlockPos exotic_worlds$playerBlockTowardTarget(BlockPos playerPos, ServerPlayer player,
            @Coerce Object target) {
        return SeamRange.toward(player, ((LinearBuildPlacementTargetAccessor) target).exotic_worlds$getHitPos(),
                playerPos);
    }
}
