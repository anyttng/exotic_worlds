package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.goals.FieryDaggerSwarmAbilityGoal;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

@Mixin(value = FieryDaggerSwarmAbilityGoal.class, remap = false)
public abstract class FieryDaggerSwarmAbilityGoalMixin {
    @WrapOperation(method = "doAction()V", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$fanTowardNearestX(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$fanTowardNearest(target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = "doAction()V", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$fanTowardNearestZ(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$fanTowardNearest(target, Direction.Axis.Z, original.call(target));
    }

    @Unique
    private double exotic_worlds$fanTowardNearest(LivingEntity target, Direction.Axis axis, double raw) {
        Mob mob = ((AnimatedActionGoalAccessor) this).exotic_worlds$getMob();
        return SeamAim.nearestCoord(mob, target, axis, raw);
    }
}
