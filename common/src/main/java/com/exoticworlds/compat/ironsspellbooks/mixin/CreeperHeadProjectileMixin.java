package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.entity.spells.creeper_head.CreeperHeadProjectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(value = CreeperHeadProjectile.class, remap = false)
public abstract class CreeperHeadProjectileMixin {
    @ModifyExpressionValue(method = InjectionTargets.PROJECTILE_ON_HIT,
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$blastedBesideHit(Vec3 position, @Local(argsOnly = true) HitResult hit) {
        return SeamRange.toward((Entity) (Object) this, hit.getLocation(), position);
    }
}
