package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;

@Mixin(ConduitBlockEntity.class)
public class ConduitBlockEntityMixin {
    @WrapOperation(
            method = "applyEffects",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private static boolean toroidal$effectRangeThroughSeam(BlockPos conduitPos, Vec3i playerPosition, double range,
            Operation<Boolean> original, @Local Player player) {
        return SeamRange.closerThan(player, conduitPos, playerPosition, range);
    }

    @WrapOperation(
            method = "updateDestroyTarget",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private static boolean toroidal$attackLeashThroughSeam(BlockPos conduitPos, Vec3i targetPosition, double range,
            Operation<Boolean> original, @Local LivingEntity targetEntity) {
        return SeamRange.closerThan(targetEntity, conduitPos, targetPosition, range);
    }
}
