package com.exoticworlds.compat.vista;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;
import com.exoticworlds.engine.seam.HeldArea;

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
        List<ChunkPos> seats = seats(packet.data(), context);
        ((ZoneSeats) packet.data()).exotic_worlds$setSentSeats(seats);
        context.clientPosition().setHeldAreas(heldAreas(packet.data(), seats));
        ExtraChunkViewData seated = seated(packet.data(), seats);
        return seated == packet.data() ? packet : new ClientBoundSyncExtraChunksPacket(seated);
    }

    static List<ChunkPos> seats(ExtraChunkViewData data, TranslationContext context) {
        List<ChunkPos> seats = new ArrayList<>();
        for (ExtraChunkViewData.Zone zone : data.getZones()) {
            seats.add(context.viewSeat(zone.center()));
        }

        return List.copyOf(seats);
    }

    static List<HeldArea> heldAreas(ExtraChunkViewData data, List<ChunkPos> seats) {
        List<ExtraChunkViewData.Zone> zones = data.getZones();
        List<HeldArea> areas = new ArrayList<>();
        for (int index = 0; index < zones.size(); index++) {
            areas.add(new HeldArea(seats.get(index), zones.get(index).radius()));
        }

        return List.copyOf(areas);
    }

    static ExtraChunkViewData seated(ExtraChunkViewData data, List<ChunkPos> seats) {
        List<ExtraChunkViewData.Zone> zones = data.getZones();
        ExtraChunkViewData seated = new ExtraChunkViewData();
        boolean moved = false;
        for (int index = 0; index < zones.size(); index++) {
            ExtraChunkViewData.Zone zone = zones.get(index);
            ChunkPos seat = seats.get(index);
            moved |= !seat.equals(zone.center());
            seated.addZone(seat, zone.radius());
        }

        return moved ? seated : data;
    }

    private ZonePacketRewriter() {
    }
}
