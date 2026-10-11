package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.accessors.ChunkPacketCopy;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;

@Mixin({ClientboundLevelChunkWithLightPacket.class, ClientboundLightUpdatePacket.class})
public class ChunkPacketCopyMixin implements Cloneable, ChunkPacketCopy {
    @Override
    public Packet<?> toroidal$copy() {
        try {
            return (Packet<?>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
