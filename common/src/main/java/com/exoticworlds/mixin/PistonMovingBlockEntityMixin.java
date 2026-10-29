package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin {
    @Unique
    private static final String MATCHES_STICKY_CRITERIA = "matchesStickyCritera";

    @WrapOperation(
            method = {"moveCollidedEntities", "fixEntityWithinPistonBase"},
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private static AABB exotic_worlds$entityBoxTowardPiston(Entity entity, Operation<AABB> original,
            @Local(argsOnly = true) BlockPos pos) {
        return FoldedBoxQuery.toward(entity, Vec3.atCenterOf(pos), original.call(entity));
    }

    @WrapOperation(
            method = MATCHES_STICKY_CRITERIA,
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private static double exotic_worlds$riderXTowardHoney(Entity entity, Operation<Double> original,
            @Local(argsOnly = true) AABB honey) {
        return NearestCopy.toward(((FoldSource) entity).exotic_worlds$wrappedFold(), Direction.Axis.X,
                honey.getCenter(), entity.position().with(Direction.Axis.X, original.call(entity)));
    }

    @WrapOperation(
            method = MATCHES_STICKY_CRITERIA,
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private static double exotic_worlds$riderZTowardHoney(Entity entity, Operation<Double> original,
            @Local(argsOnly = true) AABB honey) {
        return NearestCopy.toward(((FoldSource) entity).exotic_worlds$wrappedFold(), Direction.Axis.Z,
                honey.getCenter(), entity.position().with(Direction.Axis.Z, original.call(entity)));
    }
}
