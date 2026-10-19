package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.pasture.PastureLinkManager;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;

@Mixin(PastureLinkManager.class)
public class PastureLinkRangeMixin {
    @WrapOperation(
            method = "getLinkByPlayer(Lnet/minecraft/server/level/ServerPlayer;)Lcom/cobblemon/mod/common/api/pasture/PastureLink;",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private static boolean exotic_worlds$linkRangeThroughSeam(BlockPos pasturePos, Position playerPosition,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        return SeamRange.closerToCenterThan(player, pasturePos, playerPosition, distance);
    }
}
