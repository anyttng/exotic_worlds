package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.effect.EchoingStrikesEffect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EchoingStrikesEffect.class, remap = false)
public abstract class EchoingStrikesEffectMixin {
    @ModifyExpressionValue(method = "createEchoingArrow(Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER))
    private static Vec3 exotic_worlds$spawnBesideNearestCopy(Vec3 center,
            @Local(argsOnly = true, ordinal = 0) LivingEntity attacker) {
        return SeamSteering.nearestCopy(attacker, center);
    }
}
