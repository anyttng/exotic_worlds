package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.sable.SeamFrame;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplFrameMixin {
    @Shadow
    public ServerPlayer player;

    @WrapMethod(method = "handleMovePlayer")
    private void exotic_worlds$frameOnMove(ServerboundMovePlayerPacket packet, Operation<Void> original) {
        Object entered = SeamFrame.enter(this.player.serverLevel(), this.player, Entity::position);
        try {
            original.call(packet);
        } finally {
            SeamFrame.exit(entered);
        }
    }

    @WrapMethod(method = "handleMoveVehicle")
    private void exotic_worlds$frameOnVehicleMove(ServerboundMoveVehiclePacket packet, Operation<Void> original) {
        Object entered = SeamFrame.enter(this.player.serverLevel(), this.player, Entity::position);
        try {
            original.call(packet);
        } finally {
            SeamFrame.exit(entered);
        }
    }

    @WrapMethod(method = "handlePlayerAction")
    private void exotic_worlds$frameOnPlayerAction(ServerboundPlayerActionPacket packet, Operation<Void> original) {
        Object entered = SeamFrame.enter(this.player.serverLevel(), this.player, Entity::position);
        try {
            original.call(packet);
        } finally {
            SeamFrame.exit(entered);
        }
    }

    @WrapMethod(method = "handleUseItemOn")
    private void exotic_worlds$frameOnUseItemOn(ServerboundUseItemOnPacket packet, Operation<Void> original) {
        Object entered = SeamFrame.enter(this.player.serverLevel(), this.player, Entity::position);
        try {
            original.call(packet);
        } finally {
            SeamFrame.exit(entered);
        }
    }

    @WrapMethod(method = "handleUseItem")
    private void exotic_worlds$frameOnUseItem(ServerboundUseItemPacket packet, Operation<Void> original) {
        Object entered = SeamFrame.enter(this.player.serverLevel(), this.player, Entity::position);
        try {
            original.call(packet);
        } finally {
            SeamFrame.exit(entered);
        }
    }

    @WrapMethod(method = "handleInteract")
    private void exotic_worlds$frameOnInteract(ServerboundInteractPacket packet, Operation<Void> original) {
        Object entered = SeamFrame.enter(this.player.serverLevel(), this.player, Entity::position);
        try {
            original.call(packet);
        } finally {
            SeamFrame.exit(entered);
        }
    }
}
