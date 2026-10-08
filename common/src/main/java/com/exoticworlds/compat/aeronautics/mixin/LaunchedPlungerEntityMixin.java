package com.exoticworlds.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.aeronautics.PlungerSeamFrame;

import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = LaunchedPlungerEntity.class, remap = false)
public class LaunchedPlungerEntityMixin {
    @WrapOperation(
            method = "physicsTick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT),
            require = 1,
            expect = 1)
    private Vec3 toroidal$pullTowardTheNearestCopy(Vec3 other, Vec3 own, Operation<Vec3> original) {
        return original.call(PlungerSeamFrame.seatOther(((Entity) (Object) this).level(), own, other), own);
    }
}
