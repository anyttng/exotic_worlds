package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

@Mixin(ClientboundTeleportEntityPacket.class)
public interface TeleportEntityPacketAccessor {
    @Invoker("write")
    void exotic_worlds$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ClientboundTeleportEntityPacket exotic_worlds$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
