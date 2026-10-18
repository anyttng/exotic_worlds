package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private static final String CHECK_FALL_DAMAGE =
            "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V";

    @ModifyVariable(method = "startSleeping", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$wrapBedPosition(BlockPos bedPosition) {
        return WorldLoopAttachments.transformerOf(((LivingEntity) (Object) this).level()).fold(bedPosition);
    }

    @WrapMethod(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V")
    private void exotic_worlds$knockbackThroughSeam(double power, double xd, double zd, DamageSource source, float damage,
            boolean comesFromEffect, Operation<Void> original) {
        Vec3 direction = SeamAim.foldDelta((LivingEntity) (Object) this, xd, zd);
        original.call(power, direction.x, direction.z, source, damage, comesFromEffect);
    }

    @ModifyExpressionValue(
            method = "applyItemBlocking",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 exotic_worlds$blockConeThroughSeam(Vec3 attackDirection) {
        return SeamAim.foldDelta((LivingEntity) (Object) this, attackDirection);
    }

    @WrapOperation(
            method = "isLookingAtMe",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 exotic_worlds$gazeThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta((LivingEntity) (Object) this, original.call(x, y, z));
    }

    @ModifyVariable(
            method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z",
            at = @At("STORE"), ordinal = 1)
    private Vec3 exotic_worlds$sightTargetThroughSeam(Vec3 to) {
        return SeamSteering.nearestCopy((LivingEntity) (Object) this, to);
    }

    @ModifyExpressionValue(method = CHECK_FALL_DAMAGE, at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$landingXNearBlock(double x, @Local(argsOnly = true) BlockPos pos) {
        return exotic_worlds$nearestLandingCoordinate(Direction.Axis.X, x, pos);
    }

    @ModifyExpressionValue(method = CHECK_FALL_DAMAGE, at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$landingZNearBlock(double z, @Local(argsOnly = true) BlockPos pos) {
        return exotic_worlds$nearestLandingCoordinate(Direction.Axis.Z, z, pos);
    }

    @ModifyExpressionValue(
            method = CHECK_FALL_DAMAGE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$landingBlockNearBlock(BlockPos entityPos, @Local(argsOnly = true) BlockPos pos) {
        WorldFold transformer = ((TransformerSource) this).exotic_worlds$wrappedTransformer();
        return NearestCopy.toward(transformer, pos, entityPos);
    }

    @Unique
    private double exotic_worlds$nearestLandingCoordinate(Direction.Axis axis, double coordinate, BlockPos landingBlock) {
        WorldFold transformer = ((TransformerSource) this).exotic_worlds$wrappedTransformer();
        Vec3 position = ((LivingEntity) (Object) this).position().with(axis, coordinate);
        return NearestCopy.toward(transformer, axis, Vec3.atCenterOf(landingBlock), position);
    }
}
