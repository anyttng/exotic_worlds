package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EntityOrbitProjectile.class, remap = false)
public abstract class EntityOrbitProjectileMixin {
    @ModifyReturnValue(method = "getAngledPosition(I)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
    private Vec3 exotic_worlds$orbitPointNearProjectile(Vec3 point) {
        return SeamSteering.nearestCopy((Entity) (Object) this, point);
    }
}
