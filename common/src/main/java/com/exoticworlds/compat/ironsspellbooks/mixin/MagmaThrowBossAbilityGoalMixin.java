package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.goals.MagmaThrowBossAbilityGoal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

@Mixin(value = MagmaThrowBossAbilityGoal.class, remap = false)
public abstract class MagmaThrowBossAbilityGoalMixin {
    @Shadow
    @Final
    Mob mob;

    @ModifyExpressionValue(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_POSITION))
    private Vec3 exotic_worlds$throwAtNearestCopy(Vec3 target) {
        return SeamSteering.nearestCopy(this.mob, target);
    }
}
