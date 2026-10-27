package com.exoticworlds.compat.moonlight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.mehvahdjukaar.moonlight.api.block.IOneUserInteractable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(IOneUserInteractable.class)
public interface OneUserReachMixin {
    @WrapOperation(
            method = "isCloseEnoughToUse",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_DISTANCE_TO_SQR))
    private double exotic_worlds$userReachAcrossSeam(AABB block, Vec3 eye, Operation<Double> original,
            @Local(argsOnly = true) Entity user) {
        WorldFold fold = ((FoldSource) user).exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(block, eye);
        }

        return fold.sqrDistanceToBox(block, eye);
    }
}
