package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement",
        remap = false)
public class PortableStorageInterfaceMovementMixin {
    @Unique
    private static final String VEC_HELPER_GET_CENTER_OF =
            "Lnet/createmod/catnip/math/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = VEC_HELPER_GET_CENTER_OF))
    private Vec3 toroidal$workingTargetInTheActorFrame(Vec3 target, @Local(argsOnly = true) MovementContext context) {
        return CreateSeamFold.nearestCopy(context.world, context.position, target);
    }

    @ModifyExpressionValue(method = "findInterface", at = @At(value = "INVOKE", target = VEC_HELPER_GET_CENTER_OF))
    private Vec3 toroidal$interfaceCenterInTheActorFrame(Vec3 center,
            @Local(argsOnly = true) MovementContext context) {
        return CreateSeamFold.nearestCopy(context.world, context.position, center);
    }
}
