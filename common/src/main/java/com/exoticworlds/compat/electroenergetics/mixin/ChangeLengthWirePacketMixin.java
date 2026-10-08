package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CatnipInjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.george_vi.electroenergetics.content.wire_spool.ChangeLengthWirePacket", remap = false)
public abstract class ChangeLengthWirePacketMixin {
    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$shortSpan(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return original.call(from, WireSpan.seat(player.level(), from, to));
    }

    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = CatnipInjectionTargets.VEC_HELPER_LERP))
    private Vec3 toroidal$dropOnShortSpan(float point, Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return original.call(point, from, WireSpan.seat(player.level(), from, to));
    }
}
