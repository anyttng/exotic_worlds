package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;

@Mixin(ClientboundPlayerLookAtPacket.class)
public interface PlayerLookAtPacketAccessor {
    @Accessor("x")
    double exotic_worlds$getX();

    @Mutable
    @Accessor("x")
    void exotic_worlds$setX(double x);

    @Accessor("z")
    double exotic_worlds$getZ();

    @Mutable
    @Accessor("z")
    void exotic_worlds$setZ(double z);

    @Invoker("write")
    void exotic_worlds$write(FriendlyByteBuf output);

    @Invoker("<init>")
    static ClientboundPlayerLookAtPacket exotic_worlds$create(FriendlyByteBuf input) {
        throw new AssertionError();
    }
}
