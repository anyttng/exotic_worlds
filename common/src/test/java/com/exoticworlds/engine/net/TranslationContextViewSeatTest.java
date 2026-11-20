package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.HeldArea;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class TranslationContextViewSeatTest {
    private static final int WIDTH_IN_CHUNKS = 32;
    private static final int VIEW_DISTANCE = 8;
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final ChunkPos PLAYER_CHUNK = new ChunkPos(0, 0);
    private static final ChunkPos CHUNK_IN_THE_VIEW = new ChunkPos(5, 0);
    private static final ChunkPos CHUNK_PAST_THE_VIEW = new ChunkPos(12, 0);

    private static final Vec3 CAMERA = new Vec3(216.0, 64.0, 0.0);
    private static final ChunkPos CHUNK_BESIDE_THE_CAMERA = new ChunkPos(-14, 0);
    private static final ChunkPos ITS_CAMERA_COPY = new ChunkPos(-14 + WIDTH_IN_CHUNKS, 0);

    private static final ChunkPos CHUNK_IN_BOTH_VIEWS = new ChunkPos(-10, 0);
    private static final ChunkPos ITS_COPY_BESIDE_THE_CAMERA = new ChunkPos(-10 + WIDTH_IN_CHUNKS, 0);
    private static final ChunkPos ITS_AREA_COPY_A_LAP_BEHIND = new ChunkPos(-10 - WIDTH_IN_CHUNKS, 0);

    private static final HeldArea AREA_AT_THE_ANTIPODE = new HeldArea(new ChunkPos(WIDTH_IN_CHUNKS / 2, 0), 2);
    private static final ChunkPos CHUNK_IN_THE_AREA = new ChunkPos(-15, 0);
    private static final ChunkPos ITS_AREA_COPY = new ChunkPos(-15 + WIDTH_IN_CHUNKS, 0);

    private static TranslationContext contextOver(ClientPosition clientPosition) {
        return new TranslationContext(
                TORUS,
                clientPosition,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD,
                VIEW_DISTANCE,
                VIEW_DISTANCE,
                entityId -> null,
                entityId -> null,
                () -> {
                });
    }

    private static ClientPosition playerAtTheOrigin() {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(0.0, 0.0, Level.OVERWORLD, null, TORUS);
        return clientPosition;
    }

    @Test
    void aChunkInTheViewSeatsWhereTheDoorSeatsIt() {
        TranslationContext context = contextOver(playerAtTheOrigin());

        assertEquals(CHUNK_IN_THE_VIEW, context.viewSeat(CHUNK_IN_THE_VIEW));
        assertEquals(context.toClient(CHUNK_IN_THE_VIEW), context.viewSeat(CHUNK_IN_THE_VIEW));
    }

    @Test
    void aChunkPastTheViewSeatsWhereTheDoorSeatsIt() {
        TranslationContext context = contextOver(playerAtTheOrigin());

        assertEquals(CHUNK_PAST_THE_VIEW, context.viewSeat(CHUNK_PAST_THE_VIEW));
        assertEquals(context.toClient(CHUNK_PAST_THE_VIEW), context.viewSeat(CHUNK_PAST_THE_VIEW));
    }

    @Test
    void aChunkBesideARemoteCameraSeatsOnTheCameraCopyAsTheDoorSeatsIt() {
        ClientPosition clientPosition = playerAtTheOrigin();
        clientPosition.setHeldCacheCenter(PLAYER_CHUNK);
        clientPosition.followCamera(CAMERA, Level.OVERWORLD);
        TranslationContext context = contextOver(clientPosition);

        assertEquals(ITS_CAMERA_COPY, context.viewSeat(CHUNK_BESIDE_THE_CAMERA));
        assertEquals(context.toClient(CHUNK_BESIDE_THE_CAMERA), context.viewSeat(CHUNK_BESIDE_THE_CAMERA));
    }

    @Test
    void aChunkHeldByTheViewTheCameraAndAnAreaAnswersOneSeatPerPass() {
        ClientPosition clientPosition = playerAtTheOrigin();
        clientPosition.setHeldCacheCenter(PLAYER_CHUNK);
        clientPosition.followCamera(CAMERA, Level.OVERWORLD);
        clientPosition.setHeldAreas(List.of(new HeldArea(ITS_AREA_COPY_A_LAP_BEHIND, 2)));
        TranslationContext context = contextOver(clientPosition);

        assertEquals(CHUNK_IN_BOTH_VIEWS, context.toClient(CHUNK_IN_BOTH_VIEWS));
        assertEquals(3, context.seatPass().seats());

        context.seatPass().answer(1);
        assertEquals(ITS_COPY_BESIDE_THE_CAMERA, context.toClient(CHUNK_IN_BOTH_VIEWS));

        context.seatPass().answer(2);
        assertEquals(ITS_AREA_COPY_A_LAP_BEHIND, context.toClient(CHUNK_IN_BOTH_VIEWS));
    }

    @Test
    void aChunkOfAHeldAreaSeatsAroundTheAnchorWhileTheDoorSeatsItBesideTheArea() {
        ClientPosition clientPosition = playerAtTheOrigin();
        clientPosition.setHeldAreas(List.of(AREA_AT_THE_ANTIPODE));
        TranslationContext context = contextOver(clientPosition);

        assertEquals(CHUNK_IN_THE_AREA, context.viewSeat(CHUNK_IN_THE_AREA));
        assertEquals(ITS_AREA_COPY, context.toClient(CHUNK_IN_THE_AREA));
    }
}
