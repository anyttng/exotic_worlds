package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.util.EntityExtensionsKt;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(EntityExtensionsKt.class)
public class BlockDistanceMixin {
    @WrapOperation(
            method = "distanceTo(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)D",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private static Vec3 exotic_worlds$blockDeltaThroughSeam(Vec3 block, Vec3 body, Operation<Vec3> original,
            @Local(argsOnly = true) Entity entity) {
        return SeamAim.foldDelta(entity, original.call(block, body));
    }
}
