package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;

@Mixin(ClientboundMoveVehiclePacket.class)
public interface MoveVehiclePacketAccessor {
    @Invoker("write")
    void exotic_worlds$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ClientboundMoveVehiclePacket exotic_worlds$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
