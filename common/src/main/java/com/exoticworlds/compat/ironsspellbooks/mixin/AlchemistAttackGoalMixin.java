package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.goals.AlchemistAttackGoal;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

@Mixin(value = AlchemistAttackGoal.class, remap = false)
public abstract class AlchemistAttackGoalMixin {

    @WrapOperation(method = "doSpellAction()V",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$throwAtNearestX(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$throwAtNearest(target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = "doSpellAction()V",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$throwAtNearestZ(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$throwAtNearest(target, Direction.Axis.Z, original.call(target));
    }

    @Unique
    private double exotic_worlds$throwAtNearest(LivingEntity target, Direction.Axis axis, double raw) {
        PathfinderMob mob = ((WizardAttackGoalAccessor) this).exotic_worlds$getMob();
        return SeamAim.nearestCoord(mob, target, axis, raw);
    }
}
