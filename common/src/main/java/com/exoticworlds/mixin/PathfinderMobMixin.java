package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;

@Mixin(PathfinderMob.class)
public abstract class PathfinderMobMixin {
    @WrapOperation(
            method = "closeRangeLeashBehaviour",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double toroidal$holderXThroughSeam(Entity leashHolder, Operation<Double> original) {
        return SeamAim.nearestCoord((PathfinderMob) (Object) this, leashHolder, Direction.Axis.X,
                original.call(leashHolder));
    }

    @WrapOperation(
            method = "closeRangeLeashBehaviour",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double toroidal$holderZThroughSeam(Entity leashHolder, Operation<Double> original) {
        return SeamAim.nearestCoord((PathfinderMob) (Object) this, leashHolder, Direction.Axis.Z,
                original.call(leashHolder));
    }
}
