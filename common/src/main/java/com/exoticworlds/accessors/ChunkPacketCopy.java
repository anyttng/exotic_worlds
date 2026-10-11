package com.exoticworlds.accessors;

import net.minecraft.network.protocol.Packet;

public interface ChunkPacketCopy {
    Packet<?> toroidal$copy();
}
