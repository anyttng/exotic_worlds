package com.exoticworlds.compat.vista;

import java.util.List;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.net.TranslationContext;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.mehvahdjukaar.vista.common.chunk_tracking.ServerExtraChunkViewData;
import net.mehvahdjukaar.vista.network.ClientBoundSyncExtraChunksPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class ZoneReseat {
    public static void resendMovedZones(ServerPlayer player) {
        ServerExtraChunkViewData viewData = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player);
        List<ChunkPos> sent = ((ZoneSeats) viewData).exotic_worlds$sentSeats();
        if (sent == null || viewData.getZones().isEmpty()) {
            return;
        }

        TranslationContext context = TranslationContext.of(player, WorldLoopAttachments.foldOf(player.serverLevel()));
        if (sent.equals(ZonePacketRewriter.seats(viewData, context))) {
            return;
        }

        List<ExtraChunkViewData.Zone> zones = List.copyOf(viewData.getZones());
        viewData.clearZones();
        for (ExtraChunkViewData.Zone zone : zones) {
            viewData.addZone(zone.center(), zone.radius());
        }

        NetworkHelper.sendToClientPlayer(player, new ClientBoundSyncExtraChunksPacket(viewData));
    }

    private ZoneReseat() {
    }
}
