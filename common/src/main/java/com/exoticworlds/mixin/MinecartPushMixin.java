package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

@Mixin(AbstractMinecart.class)
public class MinecartPushMixin {
    @Unique
    private static final String exotic_worlds$PUSH = "push(Lnet/minecraft/world/entity/Entity;)V";

    @WrapOperation(method = exotic_worlds$PUSH, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double exotic_worlds$shoverNearX(Entity other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.X, original.call(other));
    }

    @WrapOperation(method = exotic_worlds$PUSH, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double exotic_worlds$shoverNearZ(Entity other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.Z, original.call(other));
    }
}
