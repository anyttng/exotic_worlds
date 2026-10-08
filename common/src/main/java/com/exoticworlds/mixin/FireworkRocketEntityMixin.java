package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.engine.seam.SeamSteering;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {
    @ModifyVariable(method = "dealExplosionDamage", at = @At("STORE"), ordinal = 1)
    private Vec3 toroidal$sightTargetThroughSeam(Vec3 to) {
        return SeamSteering.nearestCopy((FireworkRocketEntity) (Object) this, to);
    }
}
