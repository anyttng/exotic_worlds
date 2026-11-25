package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import jp.aquafactory.apprenticecodex.spell.wizardlamp.Wizardlamp;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Wizardlamp.class, remap = false)
public abstract class WizardlampMixin {
    @WrapOperation(method = "resolveClientPlacePos",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private static Vec3 exotic_worlds$placeOffsetThroughSeam(Vec3 requested, Vec3 eye, Operation<Vec3> original,
            @Local(argsOnly = true) LivingEntity entity) {
        return original.call(SeamRange.toward(entity, eye, requested), eye);
    }
}
