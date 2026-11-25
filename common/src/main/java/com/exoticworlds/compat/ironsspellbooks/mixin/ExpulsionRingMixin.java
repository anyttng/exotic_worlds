package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.item.curios.ExpulsionRing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ExpulsionRing.class, remap = false)
public abstract class ExpulsionRingMixin {
    @ModifyExpressionValue(method = "lambda$handleAbility$0(Lnet/minecraft/server/level/ServerPlayer;"
            + "Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER, ordinal = 0))
    private static Vec3 exotic_worlds$expelledBesideWearer(Vec3 center, @Local(argsOnly = true) ServerPlayer wearer) {
        return SeamSteering.nearestCopy(wearer, center);
    }
}
