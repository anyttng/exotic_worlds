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
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

@Mixin(AbstractMinecart.class)
public class MinecartPushMixin {
    @Unique
    private static final String toroidal$PUSH = "push(Lnet/minecraft/world/entity/Entity;)V";

    @Unique
    private static final String toroidal$PUSH_OTHER_MINECART =
            "pushOtherMinecart(Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;DD)V";

    @Unique
    private static final String toroidal$MINECART_GET_X =
            "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;getX()D";

    @Unique
    private static final String toroidal$MINECART_GET_Z =
            "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;getZ()D";

    @WrapOperation(method = toroidal$PUSH, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X, ordinal = 0))
    private double toroidal$shoverNearX(Entity other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.X, original.call(other));
    }

    @WrapOperation(method = toroidal$PUSH, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z, ordinal = 0))
    private double toroidal$shoverNearZ(Entity other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.Z, original.call(other));
    }

    @WrapOperation(
            method = toroidal$PUSH_OTHER_MINECART,
            at = @At(value = "INVOKE", target = toroidal$MINECART_GET_X, ordinal = 0))
    private double toroidal$otherCartNearX(AbstractMinecart other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.X, original.call(other));
    }

    @WrapOperation(
            method = toroidal$PUSH_OTHER_MINECART,
            at = @At(value = "INVOKE", target = toroidal$MINECART_GET_Z, ordinal = 0))
    private double toroidal$otherCartNearZ(AbstractMinecart other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.Z, original.call(other));
    }
}
