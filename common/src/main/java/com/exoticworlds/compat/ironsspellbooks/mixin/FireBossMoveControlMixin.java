package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossMoveControl;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

@Mixin(value = FireBossMoveControl.class, remap = false)
public abstract class FireBossMoveControlMixin extends MoveControl {
    private FireBossMoveControlMixin(Mob mob) {
        super(mob);
    }

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$steerTowardNearestX(LivingEntity target,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Double> original) {
        return SeamAim.nearestCoord(this.mob, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$steerTowardNearestZ(LivingEntity target,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Double> original) {
        return SeamAim.nearestCoord(this.mob, target, Direction.Axis.Z, original.call(target));
    }
}
