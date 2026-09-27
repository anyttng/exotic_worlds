package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.devices.device.SimulatedDevice;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.electroenergetics.WireSpan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(targets = "com.george_vi.electroenergetics.events.GameEvents", remap = false)
public abstract class BulbSpawnGuardMixin {
    @WrapOperation(method = "lambda$spawnMob$2",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private static double toroidal$spawnBesideDevice(BlockPos device, Vec3i spawn, Operation<Double> original,
            @Local(argsOnly = true) SimulatedDevice guard) {
        return original.call(device, WireSpan.seat(guard.level, device, spawn));
    }
}
