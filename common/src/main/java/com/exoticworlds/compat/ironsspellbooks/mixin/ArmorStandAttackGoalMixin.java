package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand.ArmorStandAttackGoal;

import net.minecraft.world.phys.Vec3;

@Mixin(value = ArmorStandAttackGoal.class, remap = false)
public abstract class ArmorStandAttackGoalMixin {
    @WrapOperation(method = "doMovement(D)V", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double exotic_worlds$leashThroughSeam(Vec3 spawn, Vec3 target, Operation<Double> original) {
        return original.call(spawn,
                SeamRange.toward(((WizardAttackGoalAccessor) this).exotic_worlds$getMob(), spawn, target));
    }
}
