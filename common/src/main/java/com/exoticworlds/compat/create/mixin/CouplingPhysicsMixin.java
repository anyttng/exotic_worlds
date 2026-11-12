package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.minecart.CouplingPhysics;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

@Mixin(CouplingPhysics.class)
public class CouplingPhysicsMixin {
    @WrapOperation(method = {"hardCollisionStep", "softCollisionStep"},
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private static double exotic_worlds$stressBetweenTheNearCopies(Vec3 position, Vec3 otherPosition,
            Operation<Double> original, @Local(argsOnly = true) ServerLevel world) {
        return original.call(position, CreateSeamFold.nearestCopy(world, position, otherPosition));
    }

    @WrapOperation(method = {"hardCollisionStep", "softCollisionStep"},
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT, ordinal = 0))
    private static Vec3 exotic_worlds$linkBetweenTheNearCopies(Vec3 otherPosition, Vec3 position,
            Operation<Vec3> original, @Local(argsOnly = true) ServerLevel world) {
        return original.call(CreateSeamFold.nearestCopy(world, position, otherPosition), position);
    }
}
