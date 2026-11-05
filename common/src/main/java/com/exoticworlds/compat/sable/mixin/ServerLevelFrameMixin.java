package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.sable.SeamFrame;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

@Mixin(ServerLevel.class)
public abstract class ServerLevelFrameMixin {
    @WrapMethod(method = "tickNonPassenger")
    private void exotic_worlds$frameOnEntityTick(Entity entity, Operation<Void> original) {
        Object entered = SeamFrame.enter((ServerLevel) (Object) this, entity, Entity::position);
        try {
            original.call(entity);
        } finally {
            SeamFrame.exit(entered);
        }
    }

    @WrapMethod(method = "tickPassenger")
    private void exotic_worlds$frameOnPassengerTick(Entity vehicle, Entity passenger, Operation<Void> original) {
        Object entered = SeamFrame.enter((ServerLevel) (Object) this, passenger, Entity::position);
        try {
            original.call(vehicle, passenger);
        } finally {
            SeamFrame.exit(entered);
        }
    }
}
