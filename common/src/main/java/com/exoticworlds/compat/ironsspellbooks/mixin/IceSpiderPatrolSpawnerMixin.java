package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.worldgen.IceSpiderPatrolSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

@Mixin(value = IceSpiderPatrolSpawner.class, remap = false)
public abstract class IceSpiderPatrolSpawnerMixin {
    @ModifyExpressionValue(method = "lambda$getGroupedPlayerCount$0(Lnet/minecraft/world/entity/player/Player;I"
            + "Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos exotic_worlds$playerBesideGroup(BlockPos playerPos,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) BlockPos groupPos) {
        return NearestCopy.toward(((FoldSource) player).exotic_worlds$wrappedFold(), groupPos, playerPos);
    }
}
