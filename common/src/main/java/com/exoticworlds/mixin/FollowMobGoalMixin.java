package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.phys.Vec3;

@Mixin(FollowMobGoal.class)
public class FollowMobGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getX()D"))
    private double exotic_worlds$followedXThroughSeam(Mob read, Operation<Double> original) {
        return exotic_worlds$followedThroughSeam(read, Direction.Axis.X, original.call(read));
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getZ()D"))
    private double exotic_worlds$followedZThroughSeam(Mob read, Operation<Double> original) {
        return exotic_worlds$followedThroughSeam(read, Direction.Axis.Z, original.call(read));
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/LookControl;getWantedX()D"))
    private double exotic_worlds$wantedLookXThroughSeam(LookControl lookControl, Operation<Double> original) {
        double wantedX = original.call(lookControl);
        return exotic_worlds$folds()
                ? exotic_worlds$wantedNearTheMob(wantedX, lookControl.getWantedZ()).x
                : wantedX;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/LookControl;getWantedZ()D"))
    private double exotic_worlds$wantedLookZThroughSeam(LookControl lookControl, Operation<Double> original) {
        double wantedZ = original.call(lookControl);
        return exotic_worlds$folds()
                ? exotic_worlds$wantedNearTheMob(lookControl.getWantedX(), wantedZ).z
                : wantedZ;
    }

    @Unique
    private double exotic_worlds$followedThroughSeam(Mob read, Direction.Axis axis, double coord) {
        return read == this.mob ? coord : SeamSteering.nearestCopy(this.mob, read.position().with(axis, coord)).get(axis);
    }

    @Unique
    private boolean exotic_worlds$folds() {
        return ((FoldSource) this.mob).exotic_worlds$wrappedFold() != null;
    }

    @Unique
    private Vec3 exotic_worlds$wantedNearTheMob(double wantedX, double wantedZ) {
        return SeamSteering.nearestCopy(this.mob, new Vec3(wantedX, this.mob.getY(), wantedZ));
    }
}
