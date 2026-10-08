package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private static final String CHECK_FALL_DAMAGE =
            "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V";

    @ModifyVariable(method = "startSleeping", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBedPosition(BlockPos bedPosition) {
        return WorldLoopAttachments.transformerOf(((LivingEntity) (Object) this).level()).fold(bedPosition);
    }

    @WrapMethod(method = "knockback(DDD)V")
    private void toroidal$knockbackThroughSeam(double power, double xd, double zd, Operation<Void> original) {
        Vec3 direction = SeamAim.foldDelta((LivingEntity) (Object) this, xd, zd);
        original.call(power, direction.x, direction.z);
    }

    // On 1.21.1 the shield cone is measured in isDamageSourceBlocked, off vectorTo; 26.x moved it into
    // applyItemBlocking and spells it as a subtract.
    @ModifyExpressionValue(
            method = "isDamageSourceBlocked",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;vectorTo(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$blockConeThroughSeam(Vec3 attackDirection) {
        return SeamAim.foldDelta((LivingEntity) (Object) this, attackDirection);
    }

    @ModifyVariable(
            method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("STORE"), ordinal = 1)
    private Vec3 toroidal$sightTargetThroughSeam(Vec3 to) {
        return SeamSteering.nearestCopy((LivingEntity) (Object) this, to);
    }

    @ModifyExpressionValue(method = CHECK_FALL_DAMAGE, at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double toroidal$landingXNearBlock(double x, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestLandingCoordinate(Direction.Axis.X, x, pos);
    }

    @ModifyExpressionValue(method = CHECK_FALL_DAMAGE, at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double toroidal$landingZNearBlock(double z, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestLandingCoordinate(Direction.Axis.Z, z, pos);
    }

    @ModifyExpressionValue(
            method = CHECK_FALL_DAMAGE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$landingBlockNearBlock(BlockPos entityPos, @Local(argsOnly = true) BlockPos pos) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return NearestCopy.toward(transformer, pos, entityPos);
    }

    @Unique
    private double toroidal$nearestLandingCoordinate(Direction.Axis axis, double coordinate, BlockPos landingBlock) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        Vec3 position = ((LivingEntity) (Object) this).position().with(axis, coordinate);
        return NearestCopy.toward(transformer, axis, Vec3.atCenterOf(landingBlock), position);
    }
}
