package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

@Mixin(Explosion.class)
public class ServerExplosionMixin {
    @Shadow
    @Final
    private double x;

    @Shadow
    @Final
    private double z;

    @ModifyVariable(
            method = "getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F",
            at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$exposureCentreThroughSeam(Vec3 centre, @Local(argsOnly = true) Entity entity) {
        return SeamSteering.nearestCopy(entity, centre);
    }

    @ModifyExpressionValue(
            method = "explode",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double toroidal$knockbackOriginX(double entityX, @Local Entity entity) {
        return this.x + SeamAim.foldDelta(entity, entityX - this.x, entity.getZ() - this.z).x;
    }

    @ModifyExpressionValue(
            method = "explode",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double toroidal$knockbackOriginZ(double entityZ, @Local Entity entity) {
        return this.z + SeamAim.foldDelta(entity, entity.getX() - this.x, entityZ - this.z).z;
    }
}
