package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.goals.FireWallAbilityGoal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FireWallAbilityGoal.class, remap = false)
public abstract class FireWallAbilityGoalMixin {
    @ModifyExpressionValue(method = "doAction()V",
            at = @At(value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/mobs/wizards/fire_boss/FireBossEntity;position()"
                            + "Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$bossBesideTarget(Vec3 boss, @Local LivingEntity target) {
        return SeamSteering.nearestCopy(target, boss);
    }
}
