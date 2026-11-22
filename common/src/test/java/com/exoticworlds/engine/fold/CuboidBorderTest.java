package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidEdge;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFixtures;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

class CuboidBorderTest {
    private static final CuboidAtlas ATLAS = CuboidAtlas.of(16, 24, 40,
            CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
    private static final WorldFold FOLD = CuboidFixtures.cuboidFold(ATLAS);
    private static final CuboidFace TOP = CuboidFixtures.face(ATLAS, CuboidFace.Side.TOP);
    private static final SeamTransform TOP_TO_WEST = topWestEdge().transform();
    private static final double HALF = 24.0;
    private static final double CENTER_PAST_EDGE = 8.0;
    private static final double CENTER_ALONG = TOP.depthBlocks() / 2.0;
    private static final double CENTER_X = TOP.minBlockX() + CENTER_PAST_EDGE;
    private static final double CENTER_Z = TOP.minBlockZ() + CENTER_ALONG;
    private static final CuboidBorder BORDER = (CuboidBorder) SeamBorder.of(FOLD,
            CENTER_X - HALF, CENTER_X + HALF, CENTER_Z - HALF, CENTER_Z + HALF);
    private static final double[] OUTWARDS = {0.5, 4.5, 10.5, 15.5, 16.5, 20.5, 31.5};
    private static final double[] ALONGS = {-30.5, -23.5, -0.5, 0.5, 23.5, 24.5};
    private static final int WALL_REACH = 30;
    private static final double COLLIDER_DEPTH = 2.0;
    private static final double PROBE = 0.01;
    private static final double TOLERANCE = 1.0E-4;

    private static CuboidEdge topWestEdge() {
        return ATLAS.edges().stream()
                .filter(edge -> edge.first() == CuboidFace.Side.TOP && edge.firstSide() == Direction.WEST)
                .findFirst()
                .orElseThrow();
    }

    private static Vec3 onWest(double outward, double along) {
        double[] flat = CuboidFixtures.pointBy(TOP, Direction.WEST, CENTER_ALONG + along, outward);
        double[] carried = CuboidFixtures.carried(TOP_TO_WEST, flat);
        return new Vec3(carried[0], 0.0, carried[1]);
    }

    private static Vec3 unfolded(double outward, double along) {
        return new Vec3(TOP.minBlockX() - outward, 0.0, CENTER_Z + along);
    }

    @Test
    void aCuboidFoldGetsTheCuboidForm() {
        assertInstanceOf(CuboidBorder.class, SeamBorder.of(FOLD, -HALF, HALF, -HALF, HALF));
    }

    @Test
    void aPointAcrossTheTurnedEdgeIsInsideAsItsUnfoldedTwinIs() {
        for (double outward : OUTWARDS) {
            for (double along : ALONGS) {
                Vec3 twin = unfolded(outward, along);
                boolean expected = Math.abs(twin.x - CENTER_X) < HALF && Math.abs(twin.z - CENTER_Z) < HALF;
                Vec3 west = onWest(outward, along);
                assertEquals(expected, BORDER.inside(west.x, west.z, 0.0), "west point " + west + " twin " + twin);
            }
        }
    }

    @Test
    void theDistanceAcrossTheTurnedEdgeIsTheUnfoldedOne() {
        for (double outward : OUTWARDS) {
            for (double along : ALONGS) {
                Vec3 twin = unfolded(outward, along);
                double expected = HALF - Math.max(Math.abs(twin.x - CENTER_X), Math.abs(twin.z - CENTER_Z));
                Vec3 west = onWest(outward, along);
                assertEquals(expected, BORDER.distanceToEdge(west.x, west.z), TOLERANCE, "west point " + west);
            }
        }
    }

    @Test
    void aPointOutsideAcrossTheTurnedEdgeClampsWhereItsUnfoldedTwinDoes() {
        for (double along : ALONGS) {
            Vec3 twin = unfolded(OUTWARDS[OUTWARDS.length - 1], along);
            Vec3 clampedTwin = new Vec3(
                    Math.clamp(twin.x, CENTER_X - HALF, CENTER_X + HALF),
                    twin.y,
                    Math.clamp(twin.z, CENTER_Z - HALF, CENTER_Z + HALF));
            Vec3 west = onWest(OUTWARDS[OUTWARDS.length - 1], along);
            Vec3 clamped = BORDER.clamped(west.x, west.y, west.z);
            assertNear(FOLD.fold(clampedTwin), FOLD.fold(clamped), "west point " + west);
        }
    }

    @Test
    void theWallEnclosesTheSameBlocksFromBothFacesOfTheTurnedEdge() {
        Vec3 onTop = new Vec3(TOP.minBlockX() + COLLIDER_DEPTH, 0.0, CENTER_Z);
        Vec3 onWestSide = onWest(COLLIDER_DEPTH, 0.0);
        VoxelShape fromTop = BORDER.wallAround(onTop.x, onTop.z);
        VoxelShape fromWest = BORDER.wallAround(onWestSide.x, onWestSide.z);
        int walled = 0;
        for (int dx = -WALL_REACH; dx < WALL_REACH; dx++) {
            for (int dz = -WALL_REACH; dz < WALL_REACH; dz++) {
                Vec3 topFrame = new Vec3(TOP.minBlockX() + dx + 0.5, 0.0, CENTER_Z + dz + 0.5);
                Vec3 westFrame = TOP_TO_WEST.apply(topFrame);
                boolean topWalled = walls(fromTop, topFrame);
                assertEquals(topWalled, walls(fromWest, westFrame), "block " + topFrame + " seen as " + westFrame);
                walled += topWalled ? 1 : 0;
            }
        }

        assertTrue(walled > 0 && walled < 4 * WALL_REACH * WALL_REACH, "the rig holds both walled and open blocks");
    }

    @Test
    void aBorderWiderThanTheBoxHoldsEveryFaceInside() {
        double max = WorldBorder.MAX_CENTER_COORDINATE;
        SeamBorder whole = SeamBorder.of(FOLD, -max, max, -max, max);
        for (CuboidFace face : ATLAS.faces()) {
            double x = face.minBlockX() + face.widthBlocks() / 2.0;
            double z = face.minBlockZ() + face.depthBlocks() / 2.0;
            assertTrue(whole.inside(x, z, 0.0), face.side() + " face");
        }
    }

    private static boolean walls(VoxelShape wall, Vec3 point) {
        return Shapes.joinIsNotEmpty(wall, Shapes.box(point.x - PROBE, -PROBE, point.z - PROBE,
                point.x + PROBE, PROBE, point.z + PROBE), BooleanOp.AND);
    }

    private static void assertNear(Vec3 expected, Vec3 actual, String message) {
        assertEquals(expected.x, actual.x, TOLERANCE, message);
        assertEquals(expected.z, actual.z, TOLERANCE, message);
    }
}
