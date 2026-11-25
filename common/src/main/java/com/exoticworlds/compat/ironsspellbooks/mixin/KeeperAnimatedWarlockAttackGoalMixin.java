package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.keeper.KeeperAnimatedWarlockAttackGoal;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

@Mixin(value = KeeperAnimatedWarlockAttackGoal.class, remap = false)
public abstract class KeeperAnimatedWarlockAttackGoalMixin {

    @ModifyExpressionValue(method = "handleAttackLogic(D)V", at = {
            @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_POSITION, ordinal = 0),
            @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_POSITION, ordinal = 1)})
    private Vec3 exotic_worlds$lungeAtNearestCopy(Vec3 target) {
        PathfinderMob mob = exotic_worlds$mob();
        return SeamSteering.nearestCopy(mob, target);
    }

    @WrapOperation(method = "forceFaceTarget()V",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$faceNearestX(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$faceNearest(target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = "forceFaceTarget()V",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$faceNearestZ(LivingEntity target, Operation<Double> original) {
        return exotic_worlds$faceNearest(target, Direction.Axis.Z, original.call(target));
    }

    @Unique
    private double exotic_worlds$faceNearest(LivingEntity target, Direction.Axis axis, double raw) {
        PathfinderMob mob = exotic_worlds$mob();
        return SeamAim.nearestCoord(mob, target, axis, raw);
    }

    @Unique
    private PathfinderMob exotic_worlds$mob() {
        return ((WizardAttackGoalAccessor) this).exotic_worlds$getMob();
    }
}
