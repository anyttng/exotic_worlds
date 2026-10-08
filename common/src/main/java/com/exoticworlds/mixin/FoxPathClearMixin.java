package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;

@Mixin(Fox.class)
public class FoxPathClearMixin {
    @WrapOperation(
            method = "isPathClear",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private static double toroidal$pathTargetX(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Fox fox) {
        return SeamAim.nearestCoord(fox, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(
            method = "isPathClear",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private static double toroidal$pathTargetZ(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Fox fox) {
        return SeamAim.nearestCoord(fox, target, Direction.Axis.Z, original.call(target));
    }
}
