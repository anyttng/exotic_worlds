package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidEdge;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class MirrorChartCuboidTest {
    private static final CuboidAtlas ATLAS =
            CuboidAtlas.of(24, 16, 20, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
    private static final WorldFold FOLD = CuboidFixtures.cuboidFold(ATLAS);
    private static final CuboidFace FRONT = CuboidFixtures.face(ATLAS, CuboidFace.Side.FRONT);
    private static final Vec3 VERTEX = new Vec3(FRONT.minBlockX(), 0.0, FRONT.minBlockZ());

    private static final double RADIUS_BLOCKS = 48.0;
    private static final double ARC_FIRST_DEGREES = -89.0;
    private static final double ARC_STEP_DEGREES = 2.0;
    private static final int ARC_STEPS = 135;
    private static final double LOOP_TURN_DEGREES = ARC_STEPS * ARC_STEP_DEGREES;
    private static final int LOOPS = 2;
    private static final int HOP_ON_FRONT_AFTER_A_LOOP = ARC_STEPS + 60;
    private static final Vec3 STEP_ON_THE_FACE = new Vec3(0.0, 0.0, 3.0);
    private static final int VIEW_DISTANCE_CHUNKS = 5;
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final float CLIENT_YAW = 30.0F;
    private static final double TOLERANCE = 1.0E-6;

    private static Vec3 arcPoint(int step) {
        double radians = Math.toRadians(ARC_FIRST_DEGREES + step * ARC_STEP_DEGREES);
        return VERTEX.add(RADIUS_BLOCKS * Math.cos(radians), 0.0, RADIUS_BLOCKS * Math.sin(radians));
    }

    private static Vec3 serverPointAt(int hop) {
        return arcPoint(hop % ARC_STEPS);
    }

    private static Vec3 rotated(Vec3 delta, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(delta.x * cos - delta.z * sin, delta.y, delta.x * sin + delta.z * cos);
    }

    private static Vec3 turnAfter(int hop, Vec3 serverDelta) {
        int loopsDone = hop / ARC_STEPS;
        return rotated(serverDelta, loopsDone * LOOP_TURN_DEGREES);
    }

    private static ClientPosition walkedAround(int hops) {
        ClientPosition mirror = new ClientPosition();
        Vec3 first = arcPoint(0);
        mirror.rebase(first.x, first.z, Level.OVERWORLD, FOLD);
        for (int hop = 1; hop <= hops; hop++) {
            Vec3 next = arcPoint(hop);
            mirror.set(next.x, next.z, MirrorWriter.PLAYER_MOVE);
        }
        return mirror;
    }

    private static Vec3 mirrorPoint(ClientPosition mirror) {
        return new Vec3(mirror.getX(), 0.0, mirror.getZ());
    }

    private static TranslationContext contextOf(ClientPosition mirror) {
        return new TranslationContext(FOLD, mirror,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD, VIEW_DISTANCE_CHUNKS, VIEW_DISTANCE_CHUNKS, entityId -> null, entityId -> null,
                () -> {
                });
    }

    private static void assertNear(Vec3 expected, Vec3 actual, String what) {
        assertTrue(expected.subtract(actual).horizontalDistance() <= TOLERANCE,
                what + ": expected " + expected + ", got " + actual);
    }

    @Test
    void aMirrorWalkedAroundOneVertexTakesTheTurnOfItsPathAtEveryHop() {
        ClientPosition mirror = new ClientPosition();
        Vec3 first = arcPoint(0);
        mirror.rebase(first.x, first.z, Level.OVERWORLD, FOLD);
        for (int hop = 1; hop <= LOOPS * ARC_STEPS; hop++) {
            Vec3 next = arcPoint(hop);
            mirror.set(next.x, next.z, MirrorWriter.PLAYER_MOVE);
            assertNear(turnAfter(hop, EAST), mirror.orientation(FOLD).applyToDelta(EAST), "heading at hop " + hop);
            assertNear(serverPointAt(hop), mirror.toServerFrame(FOLD, mirrorPoint(mirror)),
                    "server point at hop " + hop);
        }
    }

    @Test
    void aServerPointBesideTheMirrorLandsWhereThePathPutsItAfterALoop() {
        ClientPosition mirror = walkedAround(HOP_ON_FRONT_AFTER_A_LOOP);
        Vec3 server = serverPointAt(HOP_ON_FRONT_AFTER_A_LOOP);
        Vec3 target = server.add(STEP_ON_THE_FACE);
        Vec3 expected = mirrorPoint(mirror).add(turnAfter(HOP_ON_FRONT_AFTER_A_LOOP, STEP_ON_THE_FACE));
        TranslationContext context = contextOf(mirror);

        assertNear(expected, context.toClient(target), "toClient");
        assertNear(expected, mirror.destinationOf(FOLD, target, Set.of()), "destinationOf");
    }

    @Test
    void aServerBlockBesideTheMirrorIsSeatedAndTurnedByThePathAfterALoop() {
        ClientPosition mirror = walkedAround(HOP_ON_FRONT_AFTER_A_LOOP);
        Vec3 server = serverPointAt(HOP_ON_FRONT_AFTER_A_LOOP);
        BlockPos block = BlockPos.containing(server.add(STEP_ON_THE_FACE));
        Vec3 offset = Vec3.atCenterOf(block).subtract(server);
        Vec3 seated = mirrorPoint(mirror).add(turnAfter(HOP_ON_FRONT_AFTER_A_LOOP, new Vec3(offset.x, 0.0, offset.z)));
        TranslationContext context = contextOf(mirror);

        assertEquals(new BlockPos(Mth.floor(seated.x), block.getY(), Mth.floor(seated.z)), context.toClient(block));
        assertNear(turnAfter(HOP_ON_FRONT_AFTER_A_LOOP, EAST),
                context.toClientMove(ChunkPos.containing(block)).getOrientation().applyToDelta(EAST), "chunk turn");
    }

    @Test
    void aYawSentAfterALoopIsTurnedBackIntoTheServersFrame() {
        ClientPosition mirror = walkedAround(HOP_ON_FRONT_AFTER_A_LOOP);
        double clientRadians = Math.toRadians(CLIENT_YAW);
        Vec3 clientHeading = new Vec3(-Math.sin(clientRadians), 0.0, Math.cos(clientRadians));
        Vec3 serverHeading = rotated(clientHeading, -(HOP_ON_FRONT_AFTER_A_LOOP / ARC_STEPS) * LOOP_TURN_DEGREES);
        double expected = Math.toDegrees(Math.atan2(-serverHeading.x, serverHeading.z));

        float serverYaw = SeamMove.serverYaw(FOLD, mirror, mirrorPoint(mirror), CLIENT_YAW);

        assertTrue(Math.abs(Mth.wrapDegrees(serverYaw - expected)) <= TOLERANCE,
                "expected " + expected + ", got " + serverYaw);
    }

    @Test
    void aChunkBesideTheVertexIsForgottenAtBothCopiesTheMirrorCanHold() {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(VERTEX.x + 40.0, VERTEX.z + 8.0, Level.OVERWORLD, FOLD);
        ChunkPos west = new ChunkPos(ChunkPos.containing(BlockPos.containing(VERTEX)).x() - 1,
                ChunkPos.containing(BlockPos.containing(VERTEX)).z());
        ChunkPos throughTheTop = new DeckTransformation(topToWest().transform().inverse()).apply(west);

        List<ChunkPos> candidates = contextOf(mirror).forgetCandidates(west);

        assertNotEquals(west, throughTheTop);
        assertEquals(Set.of(west, throughTheTop), new HashSet<>(candidates));
    }

    private static CuboidEdge topToWest() {
        return ATLAS.edges().stream()
                .filter(edge -> edge.first() == CuboidFace.Side.TOP && edge.firstSide() == Direction.WEST)
                .findFirst()
                .orElseThrow();
    }
}
