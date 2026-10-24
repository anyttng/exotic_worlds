package com.exoticworlds.engine.net;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;

public final class AuxiliaryLightTranslation {
    public static void register() {
        PacketTranslator.registerClientboundChunkPayloadRewriter(AuxiliaryLightDataPayload.class,
                AuxiliaryLightTranslation::seated);
    }

    static CustomPacketPayload seated(AuxiliaryLightDataPayload payload, TranslationContext context) {
        ChunkPos clientChunk = context.toClient(payload.pos(), ChunkTraffic.AUX_LIGHT);
        if (clientChunk.equals(payload.pos())) {
            return payload;
        }

        Map<BlockPos, Byte> entries = new HashMap<>(payload.entries().size());
        payload.entries().forEach((pos, light) -> entries.put(context.transformer().reseat(pos, clientChunk), light));
        return new AuxiliaryLightDataPayload(clientChunk, entries);
    }

    private AuxiliaryLightTranslation() {
    }
}
