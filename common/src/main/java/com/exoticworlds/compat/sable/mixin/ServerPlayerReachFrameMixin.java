package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.sable.SeamFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerReachFrameMixin {
    @WrapMethod(method = "isReachableBedBlock")
    private boolean exotic_worlds$frameOnBedReach(BlockPos pos, Operation<Boolean> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Object entered = SeamFrame.enter(player.level(), player, Entity::position);
        try {
            return original.call(pos);
        } finally {
            SeamFrame.exit(entered);
        }
    }
}
