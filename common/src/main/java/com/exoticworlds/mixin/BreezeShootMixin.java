package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamRange;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.Shoot;
import net.minecraft.world.phys.Vec3;

@Mixin(Shoot.class)
public class BreezeShootMixin {
    private static final String SHOOT_TICK =
            "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/breeze/Breeze;J)V";

    @WrapOperation(
            method = "isTargetWithinRange",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private static double toroidal$shootRangeThroughSeam(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(argsOnly = true) Breeze body) {
        return SeamRange.sqr(body, from, to);
    }

    @WrapOperation(
            method = "isFacingTarget",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private static Vec3 toroidal$facingTargetThroughSeam(Vec3 targetPos, Vec3 breezePos, Operation<Vec3> original,
            @Local(argsOnly = true) Breeze breeze) {
        return original.call(SeamSteering.nearestCopy(breeze, targetPos), breezePos);
    }

    @WrapOperation(
            method = SHOOT_TICK,
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double toroidal$aimTargetX(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.nearestCoord(breeze, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(
            method = SHOOT_TICK,
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.nearestCoord(breeze, target, Direction.Axis.Z, original.call(target));
    }
}
