package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.storage.pc.link.ProximityPCLink;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(ProximityPCLink.class)
public class PCLinkRangeMixin {
    @WrapOperation(
            method = "isPermitted(Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN))
    private boolean exotic_worlds$linkRangeThroughSeam(Vec3 playerPosition, Position pcCorner, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        return SeamRange.closerThan(player, playerPosition, pcCorner, distance);
    }
}
