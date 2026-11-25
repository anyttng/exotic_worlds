package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FieryDaggerEntity.class, remap = false)
public abstract class FieryDaggerEntityMixin {
    @ModifyExpressionValue(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.AABB_GET_CENTER))
    private Vec3 exotic_worlds$steerTowardNearestCopy(Vec3 center) {
        return SeamSteering.nearestCopy((Entity) (Object) this, center);
    }
}
