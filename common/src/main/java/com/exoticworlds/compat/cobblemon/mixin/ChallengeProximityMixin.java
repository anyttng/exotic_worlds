package com.exoticworlds.compat.cobblemon.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.battles.ChallengeManager;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(ChallengeManager.class)
public class ChallengeProximityMixin {
    @WrapOperation(
            method = "validateProximity(Ljava/util/Collection;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$proximityThroughSeam(ServerPlayer player, Operation<Vec3> original,
            @Local(argsOnly = true) Collection<ServerPlayer> players) {
        return SeamSteering.nearestCopy(players.iterator().next(), original.call(player));
    }
}
