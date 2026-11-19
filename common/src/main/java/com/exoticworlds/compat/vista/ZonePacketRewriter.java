package com.exoticworlds.compat.vista;

import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.mehvahdjukaar.vista.network.ClientBoundSyncExtraChunksPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;

final class ZonePacketRewriter {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(ClientBoundSyncExtraChunksPacket.class,
                ZonePacketRewriter::seated);
    }

    private static CustomPacketPayload seated(ClientBoundSyncExtraChunksPacket packet, TranslationContext context) {
        ExtraChunkViewData seated = seated(packet.data(), context);
        return seated == packet.data() ? packet : new ClientBoundSyncExtraChunksPacket(seated);
    }

    static ExtraChunkViewData seated(ExtraChunkViewData data, TranslationContext context) {
        ExtraChunkViewData seated = new ExtraChunkViewData();
        boolean moved = false;
        for (ExtraChunkViewData.Zone zone : data.getZones()) {
            ChunkPos seat = context.viewSeat(zone.center());
            moved |= !seat.equals(zone.center());
            seated.addZone(seat, zone.radius());
        }

        return moved ? seated : data;
    }

    private ZonePacketRewriter() {
    }
}
