package com.exoticworlds.core.fold;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

class CoverReachTest {
    private static final int STEP_BLOCKS = 32;
    private static final int[] STEPS_BLOCKS = {1, 7, STEP_BLOCKS, 64};
    private static final int WIDE_CHUNKS = 64;
    private static final int NARROW_CHUNKS = 20;
    private static final int SKEW_CHUNKS = 5;
    private static final int NO_STEP = 0;

    private static int perAxis(WorldFold fold, int step) {
        return Math.max(
                fold.getBlockLattice().getX().stepsToCoverTheWorld(step),
                fold.getBlockLattice().getZ().stepsToCoverTheWorld(step));
    }

    @Test
    void aTorusIsCoveredByHalfItsWiderAxis() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidths(WIDE_CHUNKS, NARROW_CHUNKS)));

        assertEquals(WIDE_CHUNKS * CoordinateConstants.CHUNK_WIDTH_BLOCKS / 2 / STEP_BLOCKS,
                fold.stepsToCoverTheWorld(STEP_BLOCKS));
        for (int step : STEPS_BLOCKS) {
            assertEquals(perAxis(fold, step), fold.stepsToCoverTheWorld(step), "step " + step);
        }
    }

    @Test
    void aSkewedTorusIsCoveredAsItsRectangle() {
        WorldFold fold = WorldFolds.of(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidths(WIDE_CHUNKS, NARROW_CHUNKS), SKEW_CHUNKS));

        for (int step : STEPS_BLOCKS) {
            assertEquals(perAxis(fold, step), fold.stepsToCoverTheWorld(step), "step " + step);
        }
    }

    @Test
    void aCylinderIsNeverCovered() {
        WorldFold fold = WorldFolds.of(
                ShapeFixtures.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, WIDE_CHUNKS)));

        assertEquals(Integer.MAX_VALUE, fold.stepsToCoverTheWorld(STEP_BLOCKS));
    }

    @Test
    void noStepNeverCoversAWorld() {
        assertEquals(Integer.MAX_VALUE,
                WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDE_CHUNKS))).stepsToCoverTheWorld(NO_STEP));
        assertEquals(Integer.MAX_VALUE,
                CuboidFixtures.cuboidFold(CuboidFixtures.atlases()[0]).stepsToCoverTheWorld(NO_STEP));
    }

    @Test
    void aSquareOfTheCuboidReachHoldsEveryFaceFromEveryFaceCorner() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (int step : STEPS_BLOCKS) {
                int reach = fold.stepsToCoverTheWorld(step) * step;
                for (CuboidFace origin : atlas.faces()) {
                    assertHoldsEveryFace(atlas, origin.minBlockX(), origin.minBlockZ(), reach);
                    assertHoldsEveryFace(atlas, origin.minBlockX() + origin.widthBlocks() - 1,
                            origin.minBlockZ() + origin.depthBlocks() - 1, reach);
                }
            }
        }
    }

    @Test
    void theCuboidReachIsTheFirstStepCountPastTheNet() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            int side = netSideBlocks(atlas);
            for (int step : STEPS_BLOCKS) {
                int steps = fold.stepsToCoverTheWorld(step);

                assertTrue(steps * step >= side, "step " + step + " stops short of " + side);
                assertTrue((steps - 1) * step < side, "step " + step + " overshoots " + side);
            }
        }
    }

    private static void assertHoldsEveryFace(CuboidAtlas atlas, int originX, int originZ, int reach) {
        for (CuboidFace face : atlas.faces()) {
            int lastX = face.minBlockX() + face.widthBlocks() - 1;
            int lastZ = face.minBlockZ() + face.depthBlocks() - 1;

            assertTrue(originX - reach <= face.minBlockX() && originX + reach >= lastX
                    && originZ - reach <= face.minBlockZ() && originZ + reach >= lastZ,
                    () -> face.side() + " lies past " + reach + " blocks of " + originX + ", " + originZ);
        }
    }

    private static int netSideBlocks(CuboidAtlas atlas) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int endX = Integer.MIN_VALUE;
        int endZ = Integer.MIN_VALUE;
        for (CuboidFace face : atlas.faces()) {
            minX = Math.min(minX, face.minBlockX());
            minZ = Math.min(minZ, face.minBlockZ());
            endX = Math.max(endX, face.minBlockX() + face.widthBlocks());
            endZ = Math.max(endZ, face.minBlockZ() + face.depthBlocks());
        }

        return Math.max(endX - minX, endZ - minZ);
    }
}
