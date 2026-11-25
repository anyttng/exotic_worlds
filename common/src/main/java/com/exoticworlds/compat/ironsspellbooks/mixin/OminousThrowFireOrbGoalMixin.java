package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.goals.OminousThrowFireOrbGoal;

import net.minecraft.world.phys.Vec3;

@Mixin(value = OminousThrowFireOrbGoal.class, remap = false)
public abstract class OminousThrowFireOrbGoalMixin {
    @ModifyExpressionValue(method = "doAction()V",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_POSITION))
    private Vec3 exotic_worlds$throwAtNearestCopy(Vec3 target) {
        return SeamSteering.nearestCopy(((AnimatedActionGoalAccessor) this).exotic_worlds$getMob(), target);
    }
}
