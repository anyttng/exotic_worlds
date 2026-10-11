package com.exoticworlds.engine.net;

import java.util.HashMap;
import java.util.Map;

import com.exoticworlds.api.v1.net.PacketRewriters;
import com.exoticworlds.api.v1.net.SeamContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;

public final class AuxiliaryLightTranslation {
    public static void register() {
        PacketRewriters.registerClientboundPayload(AuxiliaryLightDataPayload.class, AuxiliaryLightTranslation::seated);
    }

    static CustomPacketPayload seated(AuxiliaryLightDataPayload payload, SeamContext context) {
        ChunkPos clientChunk = context.toClient(payload.pos());
        if (clientChunk.equals(payload.pos())) {
            return payload;
        }

        Map<BlockPos, Byte> entries = new HashMap<>(payload.entries().size());
        payload.entries().forEach((pos, light) -> entries.put(context.toClient(pos), light));
        return new AuxiliaryLightDataPayload(clientChunk, entries);
    }

    private AuxiliaryLightTranslation() {
    }
}
