package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.simulation.RequestVoltageDataPacket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.electroenergetics.WireSpan;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = RequestVoltageDataPacket.class, remap = false)
public abstract class RequestVoltageDataPacketMixin {
    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double toroidal$playerBesideNode(Vec3 node, Vec3 player, Operation<Double> original,
            @Local(argsOnly = true) ServerPlayer requester) {
        return original.call(node, WireSpan.seat(requester.level(), node, player));
    }
}
