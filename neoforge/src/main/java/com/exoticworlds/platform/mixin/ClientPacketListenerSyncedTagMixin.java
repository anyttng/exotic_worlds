package com.exoticworlds.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.client.engine.SyncedTagFold;
import com.exoticworlds.mixin.BlockEntityDataPacketAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerSyncedTagMixin {
    @WrapOperation(
            method = "lambda$handleBlockEntityData$5",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;onDataPacket("
                    + "Lnet/minecraft/network/Connection;"
                    + "Lnet/minecraft/network/protocol/game/ClientboundBlockEntityDataPacket;"
                    + "Lnet/minecraft/core/HolderLookup$Provider;)V"))
    private void toroidal$seatSyncedPositions(BlockEntity blockEntity, Connection connection,
            ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries, Operation<Void> original) {
        CompoundTag tag = packet.getTag();
        CompoundTag seated = SyncedTagFold.inFrameOf(blockEntity, tag);
        ClientboundBlockEntityDataPacket delivered = seated == tag
                ? packet
                : BlockEntityDataPacketAccessor.toroidal$create(packet.getPos(), packet.getType(), seated);
        original.call(blockEntity, connection, delivered, registries);
    }
}
