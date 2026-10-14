package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.packagePort.PackagePortPlacementPacket;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = PackagePortPlacementPacket.class, remap = false)
public class PackagePortPlacementPacketMixin {
    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN))
    private boolean toroidal$foldPortRange(Vec3 target, Position port, double range, Operation<Boolean> original,
            ServerPlayer player) {
        if (player == null || !(port instanceof Vec3 anchor)) {
            return original.call(target, port, range);
        }

        return original.call(CreateSeamFold.nearestCopy(player.level(), anchor, target), port, range);
    }
}
