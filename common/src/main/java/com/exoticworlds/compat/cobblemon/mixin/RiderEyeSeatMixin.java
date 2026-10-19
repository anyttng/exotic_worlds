package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.cobblemon.mod.common.net.serverhandling.riding.ServerboundUpdateRiderRotationHandler;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerboundUpdateRiderRotationHandler.class)
public class RiderEyeSeatMixin {
    @ModifyArg(
            method = "handle(Lcom/cobblemon/mod/common/net/messages/server/riding/ServerboundUpdateRiderRotationPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/cobblemon/mod/common/duck/RidePassenger;cobblemon$setRideEyePos(Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 exotic_worlds$seatEyeBesideBody(Vec3 eye, @Local(argsOnly = true) ServerPlayer player) {
        return SeamSteering.nearestCopy(player, eye);
    }
}
