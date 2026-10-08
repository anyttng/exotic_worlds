package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

@Mixin(Projectile.class)
public class ProjectileMixin {
    @ModifyExpressionValue(
            method = "isOutsideOwnerCollisionRange",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"))
    private AABB toroidal$ownerRangeThroughSeam(AABB sweep, @Local Entity owner) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return FoldedBoxQuery.toward(transformer, owner.position(), sweep);
    }
}
