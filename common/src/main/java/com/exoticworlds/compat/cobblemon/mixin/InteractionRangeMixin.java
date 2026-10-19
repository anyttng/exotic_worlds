package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.net.serverhandling.RequestInteractionsHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(RequestInteractionsHandler.class)
public class InteractionRangeMixin {
    @WrapOperation(
            method = "handle(Lcom/cobblemon/mod/common/net/messages/server/RequestPlayerInteractionsPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double exotic_worlds$rangeThroughSeam(Vec3 target, Vec3 requester, Operation<Double> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return SeamRange.sqr(player, target, requester);
    }
}
