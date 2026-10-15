package com.exoticworlds.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.aeronautics.PlungerSeamFrame;

import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = LaunchedPlungerEntity.class, remap = false)
public class LaunchedPlungerEntityMixin {
    @Unique
    private static final String DEFLECTION = "lambda$static$0";

    @WrapOperation(
            method = "physicsTick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT),
            require = 1,
            expect = 1)
    private Vec3 toroidal$pullTowardTheNearestCopy(Vec3 other, Vec3 own, Operation<Vec3> original) {
        return original.call(PlungerSeamFrame.seatOther(((Entity) (Object) this).level(), own, other), own);
    }

    @WrapOperation(
            method = DEFLECTION,
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT),
            require = 1,
            expect = 1)
    private static Vec3 toroidal$deflectTowardTheNearestCopy(Vec3 target, Vec3 own, Operation<Vec3> original,
            @Local(argsOnly = true) Entity plunger) {
        return original.call(PlungerSeamFrame.seatOther(plunger.level(), own, target), own);
    }
}
