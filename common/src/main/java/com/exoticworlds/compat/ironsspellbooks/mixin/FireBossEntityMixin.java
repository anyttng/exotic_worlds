package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FireBossEntity.class, remap = false)
public abstract class FireBossEntityMixin {
    @ModifyExpressionValue(method = "handleHalfHealthSequence()V",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_POSITION))
    private Vec3 exotic_worlds$aimFireballAtNearestCopy(Vec3 target) {
        return SeamSteering.nearestCopy((Entity) (Object) this, target);
    }

    // The first read is the null test, and a null position cannot be seated.
    @ModifyExpressionValue(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/DamageSource;getSourcePosition()"
                            + "Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 exotic_worlds$parryNearestSource(Vec3 source) {
        return SeamSteering.nearestCopy((Entity) (Object) this, source);
    }

    @WrapOperation(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double exotic_worlds$stuckStepThroughSeam(Vec3 oldStuckPos, Vec3 stuckPos, Operation<Double> original) {
        return original.call(SeamSteering.nearestCopy((Entity) (Object) this, oldStuckPos), stuckPos);
    }
}
