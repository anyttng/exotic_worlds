package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.api.util.ANExplosion;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ANExplosion.class, remap = false)
public abstract class ANExplosionMixin {
    @ModifyExpressionValue(method = "explode()V", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double exotic_worlds$pushOriginX(double entityX, @Local Entity entity) {
        Vec3 center = ((Explosion) (Object) this).center();
        return center.x + SeamAim.foldDelta(entity, entityX - center.x, entity.getZ() - center.z).x;
    }

    @ModifyExpressionValue(method = "explode()V", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double exotic_worlds$pushOriginZ(double entityZ, @Local Entity entity) {
        Vec3 center = ((Explosion) (Object) this).center();
        return center.z + SeamAim.foldDelta(entity, entity.getX() - center.x, entityZ - center.z).z;
    }
}
