package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.molang.function.LivingEntityMoLangFunctions;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntityMoLangFunctions.class)
public class CanSeeAimMixin {
    @WrapOperation(
            method = "moLangFunctions$lambda$28(Lnet/minecraft/world/entity/LivingEntity;Lcom/bedrockk/molang/runtime/MoParams;)Ljava/lang/Object;",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private static Vec3 exotic_worlds$aimThroughSeam(Vec3 targetEye, Vec3 eye, Operation<Vec3> original,
            @Local(argsOnly = true) LivingEntity entity) {
        return SeamAim.foldDelta(entity, original.call(targetEye, eye));
    }
}
