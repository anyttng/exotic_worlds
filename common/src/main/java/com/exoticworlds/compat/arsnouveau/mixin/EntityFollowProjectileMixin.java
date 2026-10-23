package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EntityFollowProjectile.class, remap = false)
public abstract class EntityFollowProjectileMixin {
    @ModifyExpressionValue(method = "tick()V", at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 exotic_worlds$targetDeltaThroughSeam(Vec3 delta) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedTransformerOfReader(((Entity) (Object) this).level()),
                Vec3.ZERO, delta);
    }
}
