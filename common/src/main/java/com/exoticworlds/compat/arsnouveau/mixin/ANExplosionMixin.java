package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.engine.fold.NearestCopy;
import com.hollingsworth.arsnouveau.api.util.ANExplosion;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ANExplosion.class, remap = false)
public abstract class ANExplosionMixin {
    @ModifyExpressionValue(method = "explode()V", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double exotic_worlds$pushOriginX(double entityX, @Local Entity entity) {
        return NearestCopy.toward(((FoldSource) entity).exotic_worlds$wrappedFold(), Direction.Axis.X,
                ((Explosion) (Object) this).center(), new Vec3(entityX, entity.getY(), entity.getZ()));
    }

    @ModifyExpressionValue(method = "explode()V", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double exotic_worlds$pushOriginZ(double entityZ, @Local Entity entity) {
        return NearestCopy.toward(((FoldSource) entity).exotic_worlds$wrappedFold(), Direction.Axis.Z,
                ((Explosion) (Object) this).center(), new Vec3(entity.getX(), entity.getY(), entityZ));
    }
}
