package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = AbstractMagicProjectile.class, remap = false)
public abstract class AbstractMagicProjectileMixin {
    @ModifyVariable(method = "homeTowards(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), argsOnly = true)
    private Vec3 exotic_worlds$homeTowardNearestCopy(Vec3 target) {
        return SeamSteering.nearestCopy((Entity) (Object) this, target);
    }

    @ModifyExpressionValue(method = "handleEntityHoming()V",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER))
    private Vec3 exotic_worlds$wantNearestTargetCopy(Vec3 center) {
        return SeamSteering.nearestCopy((Entity) (Object) this, center);
    }

    @ModifyExpressionValue(method = "handleCursorHoming()V",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$measureNearestOwnerCopy(Vec3 owner) {
        return SeamSteering.nearestCopy((Entity) (Object) this, owner);
    }

    @ModifyExpressionValue(method = "lambda$tryRedirectFromEntityRicochet$2(Lnet/minecraft/world/entity/Entity;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER))
    private Vec3 exotic_worlds$coneTowardNearestCopy(Vec3 center) {
        return SeamSteering.nearestCopy((Entity) (Object) this, center);
    }

    @ModifyExpressionValue(method = "tryRedirectFromEntityRicochet(Lnet/minecraft/world/phys/EntityHitResult;)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER))
    private Vec3 exotic_worlds$ricochetAtNearestCopy(Vec3 center) {
        return SeamSteering.nearestCopy((Entity) (Object) this, center);
    }
}
