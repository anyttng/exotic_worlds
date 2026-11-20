package com.exoticworlds.compat.vista;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static com.exoticworlds.compat.CompatFoldFixture.WORLD_CHUNKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.engine.net.TranslationContext;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.HeldArea;

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
        ExtraChunkViewData sent = seated(zones(CENTER_INLAND, CENTER_ACROSS_THE_SEAM));
        assertEquals(List.of(zone(CENTER_INLAND), zone(ITS_CLIENT_COPY)), sent.getZones());
    }

    @Test
    void theServersOwnZonesStayCanonical() {
        ExtraChunkViewData raw = zones(CENTER_ACROSS_THE_SEAM);
        seated(raw);
        assertEquals(List.of(zone(CENTER_ACROSS_THE_SEAM)), raw.getZones());
    }

    @Test
    void zonesInThePlayersOwnFrameTravelUntouched() {
        ExtraChunkViewData raw = zones(CENTER_INLAND);
        assertSame(raw, seated(raw));
    }

    @Test
    void theSeatsNameTheCopyEachZoneWasSentOn() {
        assertEquals(List.of(CENTER_INLAND, ITS_CLIENT_COPY),
                ZonePacketRewriter.seats(zones(CENTER_INLAND, CENTER_ACROSS_THE_SEAM), context()));
    }

    @Test
    void eachZoneIsHeldAtItsSeatWithItsOwnRadius() {
        ExtraChunkViewData raw = zones(CENTER_INLAND, CENTER_ACROSS_THE_SEAM);
        assertEquals(List.of(new HeldArea(CENTER_INLAND, ZONE_RADIUS), new HeldArea(ITS_CLIENT_COPY, ZONE_RADIUS)),
                ZonePacketRewriter.heldAreas(raw, ZonePacketRewriter.seats(raw, context())));
    }

    @Test
    void aPacketWithNoZonesHoldsNoArea() {
        assertEquals(List.of(), ZonePacketRewriter.heldAreas(new ExtraChunkViewData(), List.of()));
    }

    private static ExtraChunkViewData seated(ExtraChunkViewData data) {
        return ZonePacketRewriter.seated(data, ZonePacketRewriter.seats(data, context()));
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
