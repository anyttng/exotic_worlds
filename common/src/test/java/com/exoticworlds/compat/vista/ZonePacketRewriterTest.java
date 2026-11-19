package com.exoticworlds.compat.vista;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_CHUNKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.engine.net.TranslationContext;
import com.exoticworlds.engine.seam.ClientPosition;

import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

class ZonePacketRewriterTest {
    private static final int VIEW_DISTANCE = 4;
    private static final int ZONE_RADIUS = 3;

    private static final double PLAYER_X = 200.0;
    private static final double PLAYER_Z = 8.0;

    private static final ChunkPos CENTER_ACROSS_THE_SEAM = new ChunkPos(-14, 0);
    private static final ChunkPos ITS_CLIENT_COPY = new ChunkPos(-14 + 2 * WORLD_CHUNKS, 0);
    private static final ChunkPos CENTER_INLAND = new ChunkPos(4, 0);

    @Test
    void aZoneAcrossTheSeamIsCenteredOnTheCopyTheClientHolds() {
        ExtraChunkViewData sent = ZonePacketRewriter.seated(zones(CENTER_INLAND, CENTER_ACROSS_THE_SEAM), context());
        assertEquals(List.of(zone(CENTER_INLAND), zone(ITS_CLIENT_COPY)), sent.getZones());
    }

    @Test
    void theServersOwnZonesStayCanonical() {
        ExtraChunkViewData raw = zones(CENTER_ACROSS_THE_SEAM);
        ZonePacketRewriter.seated(raw, context());
        assertEquals(List.of(zone(CENTER_ACROSS_THE_SEAM)), raw.getZones());
    }

    @Test
    void zonesInThePlayersOwnFrameTravelUntouched() {
        ExtraChunkViewData raw = zones(CENTER_INLAND);
        assertSame(raw, ZonePacketRewriter.seated(raw, context()));
    }

    private static ExtraChunkViewData zones(ChunkPos... centers) {
        ExtraChunkViewData data = new ExtraChunkViewData();
        for (ChunkPos center : centers) {
            data.addZone(center, ZONE_RADIUS);
        }

        return data;
    }

    private static ExtraChunkViewData.Zone zone(ChunkPos center) {
        return new ExtraChunkViewData.Zone(center, (byte) ZONE_RADIUS);
    }

    private static TranslationContext context() {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(PLAYER_X, PLAYER_Z, Level.OVERWORLD, null, PER_AXIS);
        return new TranslationContext(PER_AXIS, mirror, capacity -> {
            throw new UnsupportedOperationException();
        }, Level.OVERWORLD, VIEW_DISTANCE, VIEW_DISTANCE, entityId -> null, entityId -> null, () -> {});
    }
}
