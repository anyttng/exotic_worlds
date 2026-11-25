package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.spells.ice_block.IceBlockProjectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = IceBlockProjectile.class, remap = false)
public abstract class IceBlockProjectileMixin {
    @ModifyExpressionValue(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$driftTowardNearestCopy(Vec3 target) {
        return SeamSteering.nearestCopy((Entity) (Object) this, target);
    }
}
