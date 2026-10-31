package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamRange;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

@Mixin(EnderDragon.class)
public class EnderDragonMixin {
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;"
                            + "getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"))
    private @Nullable Vec3 exotic_worlds$flyTargetThroughSeam(@Nullable Vec3 targetLocation) {
        EnderDragon self = (EnderDragon) (Object) this;
        return targetLocation == null ? null : SeamSteering.nearestCopy(self, targetLocation);
    }

    @WrapOperation(
            method = "getHeadLookVector",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_TO_CENTER_SQR))
    private double exotic_worlds$eggDistanceThroughSeam(BlockPos eggPos, Position dragonPosition,
            Operation<Double> original) {
        return original.call(eggPos, SeamRange.towardCenterOf((EnderDragon) (Object) this, eggPos, dragonPosition));
    }

    @WrapOperation(
            method = "findClosestNode(DDD)I",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/pathfinder/Node;distanceToSqr(Lnet/minecraft/world/level/pathfinder/Node;)F"))
    private float exotic_worlds$nodeDistanceThroughSeam(Node node, Node point, Operation<Float> original) {
        BlockPos pointPos = point.asBlockPos();
        BlockPos near = SeamRange.toward((EnderDragon) (Object) this, node.asBlockPos(), pointPos);
        return original.call(node, near == pointPos ? point : point.cloneAndMove(near.getX(), near.getY(), near.getZ()));
    }

    @WrapOperation(
            method = "knockBack",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double exotic_worlds$shovedEntityX(Entity target, Operation<Double> original) {
        return SeamAim.nearestCoord((EnderDragon) (Object) this, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(
            method = "knockBack",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double exotic_worlds$shovedEntityZ(Entity target, Operation<Double> original) {
        return SeamAim.nearestCoord((EnderDragon) (Object) this, target, Direction.Axis.Z, original.call(target));
    }
}
