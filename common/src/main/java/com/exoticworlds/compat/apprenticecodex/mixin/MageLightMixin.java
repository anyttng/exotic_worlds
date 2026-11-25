package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = MageLight.class, remap = false)
public abstract class MageLightMixin {
    @WrapOperation(method = "resolveCastTarget",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double exotic_worlds$hitDistanceThroughSeam(Vec3 eye, Vec3 hit, Operation<Double> original,
            @Local(argsOnly = true) LivingEntity entity) {
        return original.call(eye, SeamRange.toward(entity, eye, hit));
    }
}
