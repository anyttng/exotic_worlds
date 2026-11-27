package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VertexLoopWalk {
    public static final CuboidAtlas ATLAS =
            CuboidAtlas.of(24, 16, 20, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
    public static final WorldFold FOLD = CuboidFixtures.cuboidFold(ATLAS);
    private static final CuboidFace FRONT = CuboidFixtures.face(ATLAS, CuboidFace.Side.FRONT);
    public static final Vec3 VERTEX = new Vec3(FRONT.minBlockX(), 0.0, FRONT.minBlockZ());

    private static final double RADIUS_BLOCKS = 48.0;
    private static final double ARC_FIRST_DEGREES = -89.0;
    private static final double ARC_STEP_DEGREES = 2.0;
    public static final int ARC_STEPS = 135;
    public static final double LOOP_TURN_DEGREES = ARC_STEPS * ARC_STEP_DEGREES;
    public static final int HOP_ON_FRONT_AFTER_A_LOOP = ARC_STEPS + 60;
    public static final Vec3 STEP_ON_THE_FACE = new Vec3(0.0, 0.0, 3.0);
    public static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final int VIEW_DISTANCE_CHUNKS = 5;
    private static final double TOLERANCE = 1.0E-6;

    public static Vec3 arcPoint(int step) {
        double radians = Math.toRadians(ARC_FIRST_DEGREES + step * ARC_STEP_DEGREES);
        return VERTEX.add(RADIUS_BLOCKS * Math.cos(radians), 0.0, RADIUS_BLOCKS * Math.sin(radians));
    }

    public static Vec3 serverPointAt(int hop) {
        return arcPoint(hop % ARC_STEPS);
    }

    public static Vec3 rotated(Vec3 delta, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(delta.x * cos - delta.z * sin, delta.y, delta.x * sin + delta.z * cos);
    }

    public static Vec3 turnAfter(int hop, Vec3 serverDelta) {
        int loopsDone = hop / ARC_STEPS;
        return rotated(serverDelta, loopsDone * LOOP_TURN_DEGREES);
    }

    public static ClientPosition walkedAround(int hops) {
        ClientPosition mirror = new ClientPosition();
        Vec3 first = arcPoint(0);
        mirror.rebase(first.x, first.z, Level.OVERWORLD, FOLD);
        for (int hop = 1; hop <= hops; hop++) {
            Vec3 next = arcPoint(hop);
            mirror.set(next.x, next.z, MirrorWriter.PLAYER_MOVE);
        }
        return mirror;
    }

    public static Vec3 mirrorPoint(ClientPosition mirror) {
        return new Vec3(mirror.getX(), 0.0, mirror.getZ());
    }

    public static TranslationContext contextOf(ClientPosition mirror) {
        return new TranslationContext(FOLD, mirror,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD, VIEW_DISTANCE_CHUNKS, VIEW_DISTANCE_CHUNKS, entityId -> null, entityId -> null,
                () -> {
                });
    }

    public static void assertNear(Vec3 expected, Vec3 actual, String what) {
        assertTrue(expected.subtract(actual).horizontalDistance() <= TOLERANCE,
                what + ": expected " + expected + ", got " + actual);
    }

    private VertexLoopWalk() {
    }
}
