package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.world.phys.Vec3;

@Mixin(DragonStrafePlayerPhase.class)
public class DragonStrafePlayerPhaseMixin {
    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$aimTargetX(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestCoord(((DragonPhaseAccessor) this).exotic_worlds$dragon(),
                target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$aimTargetZ(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestCoord(((DragonPhaseAccessor) this).exotic_worlds$dragon(),
                target, Direction.Axis.Z, original.call(target));
    }

    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR_XYZ))
    private double exotic_worlds$targetWindowThroughSeam(Vec3 target, double x, double y, double z,
            Operation<Double> original) {
        Vec3 near = SeamRange.toward(((DragonPhaseAccessor) this).exotic_worlds$dragon(), target, new Vec3(x, y, z));
        return original.call(target, near.x, near.y, near.z);
    }
}
