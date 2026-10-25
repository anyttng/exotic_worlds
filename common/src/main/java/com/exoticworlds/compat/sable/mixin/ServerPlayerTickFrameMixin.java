package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.compat.sable.SeamFrame;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTickFrameMixin {
    @WrapMethod(method = "doTick")
    private void exotic_worlds$frameOnPlayerTick(Operation<Void> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        SeamFrame.with(player.serverLevel(), player, Entity::position, original);
    }
}
