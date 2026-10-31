package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.phys.Vec3;

@Mixin(DragonChargePlayerPhase.class)
public class DragonChargePlayerPhaseMixin {
    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR_XYZ))
    private double exotic_worlds$targetWindowThroughSeam(Vec3 target, double x, double y, double z,
            Operation<Double> original) {
        Vec3 near = SeamRange.toward(((DragonPhaseAccessor) this).exotic_worlds$dragon(), target, new Vec3(x, y, z));
        return original.call(target, near.x, near.y, near.z);
    }
}
