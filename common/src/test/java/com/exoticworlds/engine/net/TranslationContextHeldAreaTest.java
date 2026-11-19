package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.HeldArea;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class TranslationContextHeldAreaTest {
    private static final int WIDTH_IN_CHUNKS = 128;
    private static final int VIEW_DISTANCE = 12;
    private static final int RADIUS = 4;

    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final double PLAYER_X = 8.0;
    private static final ChunkPos ANTIPODE_SEAT = new ChunkPos(WIDTH_IN_CHUNKS / 2, 0);
    private static final ChunkPos OUTSIDE_EVERY_AREA = new ChunkPos(40, 0);
    private static final ChunkPos IN_VIEW = new ChunkPos(5, 0);
    private static final ChunkPos IN_VIEW_SEAT_A_LAP_AWAY = new ChunkPos(5 + WIDTH_IN_CHUNKS, 0);

    private static TranslationContext contextHolding(HeldArea... areas) {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(PLAYER_X, 0.0, Level.OVERWORLD, TORUS);
        clientPosition.setHeldAreas(List.of(areas));
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

    private static TranslationContext contextHoldingTheAntipode() {
        return contextHolding(new HeldArea(ANTIPODE_SEAT, RADIUS));
    }

    private static List<ChunkPos> antipodeAreaSeats() {
        return IntStream.rangeClosed(-RADIUS, RADIUS)
                .mapToObj(dx -> new ChunkPos(ANTIPODE_SEAT.x() + dx, 0))
                .toList();
    }

    private static ChunkPos serverChunkOf(ChunkPos seat) {
        return TORUS.fold(seat);
    }

    @Test
    void everyChunkOfAnAreaStraddlingTheAntipodeSeatsBesideTheAreaSeat() {
        TranslationContext context = contextHoldingTheAntipode();
        for (ChunkPos seat : antipodeAreaSeats()) {
            assertEquals(seat, context.toClient(serverChunkOf(seat)));
        }
    }

    @Test
    void everyBlockOfAnAreaStraddlingTheAntipodeSeatsBesideTheAreaSeat() {
        TranslationContext context = contextHoldingTheAntipode();
        for (ChunkPos seat : antipodeAreaSeats()) {
            BlockPos server = serverChunkOf(seat).getMiddleBlockPosition(64);
            assertEquals(seat.getMiddleBlockPosition(64), context.toClient(server));
        }
    }

    @Test
    void aForgetOfAnAreaChunkReachesTheCopyBesideTheAreaSeat() {
        TranslationContext context = contextHoldingTheAntipode();
        for (ChunkPos seat : antipodeAreaSeats()) {
            assertTrue(context.forgetCandidates(serverChunkOf(seat)).contains(seat), seat::toString);
        }
    }

    @Test
    void anEntityInsideAnAreaSeatsBesideTheAreaSeat() {
        TranslationContext context = contextHoldingTheAntipode();
        for (ChunkPos seat : antipodeAreaSeats()) {
            Vec3 server = Vec3.atCenterOf(serverChunkOf(seat).getMiddleBlockPosition(64));
            assertEquals(Vec3.atCenterOf(seat.getMiddleBlockPosition(64)),
                    context.toClient(server, context.trackedReach()));
        }
    }

    @Test
    void aChunkOutsideEveryAreaSeatsAroundTheAnchor() {
        TranslationContext held = contextHoldingTheAntipode();
        TranslationContext free = contextHolding();
        assertEquals(free.toClient(OUTSIDE_EVERY_AREA), held.toClient(OUTSIDE_EVERY_AREA));
    }

    @Test
    void anAreaChunkInsideTheHeldViewKeepsTheViewCopy() {
        TranslationContext context = contextHolding(new HeldArea(IN_VIEW_SEAT_A_LAP_AWAY, RADIUS));
        assertEquals(IN_VIEW, context.toClient(IN_VIEW));
        Vec3 inView = Vec3.atCenterOf(IN_VIEW.getMiddleBlockPosition(64));
        assertEquals(inView, context.toClient(inView, context.trackedReach()));
    }

    @Test
    void aRebaseDropsTheHeldAreas() {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.setHeldAreas(List.of(new HeldArea(ANTIPODE_SEAT, RADIUS)));
        clientPosition.rebase(PLAYER_X, 0.0, Level.OVERWORLD, TORUS);
        assertTrue(clientPosition.heldAreas().isEmpty());
    }

    @Test
    void theAreaStraddlesTheAntipode() {
        TranslationContext free = contextHolding();
        List<Integer> laps = antipodeAreaSeats().stream()
                .map(seat -> free.toClient(serverChunkOf(seat)).x() - seat.x())
                .distinct()
                .toList();
        assertEquals(2, laps.size(), laps::toString);
    }
}
