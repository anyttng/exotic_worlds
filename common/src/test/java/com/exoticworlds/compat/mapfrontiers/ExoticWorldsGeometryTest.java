package com.exoticworlds.compat.mapfrontiers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class ExoticWorldsGeometryTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH = (MAX_CHUNK - MIN_CHUNK) * 16;
    private static final int SKEW_CHUNKS = 3;
    private static final int SKEW = SKEW_CHUNKS * 16;
    private static final int Y = 64;

    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK);
    private static final WorldLoopBounds BOTH = new WorldLoopBounds(LOOPED, LOOPED);
    private static final WorldLoopBounds X_ONLY = new WorldLoopBounds(LOOPED, AxisBounds.Unbounded.INSTANCE);

    private static ExoticWorldsGeometry torus() {
        return geometry(ShapeFixtures.torus(BOTH));
    }

    private static ExoticWorldsGeometry latticeTorus() {
        return geometry(FlatShape.latticeTorus(BOTH, SKEW_CHUNKS));
    }

    private static ExoticWorldsGeometry cylinder() {
        return geometry(ShapeFixtures.cylinder(X_ONLY));
    }

    private static ExoticWorldsGeometry geometry(FlatShape shape) {
        return new ExoticWorldsGeometry(TestShapes.of(WorldFolds.of(shape)));
    }

    private static List<BlockPos> copiesInBounds(ExoticWorldsGeometry geometry, BlockPos point, double minX,
            double maxX, double minZ, double maxZ) {
        List<BlockPos> copies = new ArrayList<>();
        assertFalse(geometry.anyCopyInBounds(point, minX, maxX, minZ, maxZ, copy -> !copies.add(copy)),
                "a test that never accepts was answered true");
        return copies;
    }

    @Nested
    class Periods {
        @Test
        void theTorusStepsOneWorldOnBothAxes() {
            ExoticWorldsGeometry geometry = torus();
            assertEquals(WIDTH, geometry.periodX(), "x period");
            assertEquals(WIDTH, geometry.periodZ(), "z period");
            assertTrue(geometry.hasWrappedAxes(), "the torus reads as flat");
        }

        @Test
        void theLatticeTorusHasNoPeriodButStillWraps() {
            ExoticWorldsGeometry geometry = latticeTorus();
            assertEquals(0, geometry.periodX(), "a per-axis period on a shape whose z seam moves x");
            assertEquals(0, geometry.periodZ(), "a per-axis period on a shape whose z seam moves x");
            assertTrue(geometry.hasWrappedAxes(), "the lattice torus reads as flat");
        }

        @Test
        void theCylinderStepsOnItsLoopedAxisAlone() {
            ExoticWorldsGeometry geometry = cylinder();
            assertEquals(WIDTH, geometry.periodX(), "x period");
            assertEquals(0, geometry.periodZ(), "the open z axis got a period");
            assertTrue(geometry.hasWrappedAxes(), "the cylinder reads as flat");
        }
    }

    @Nested
    class NearestCopies {
        @Test
        void aVertexPastTheXSeamIsSeatedBesideTheCursor() {
            assertEquals(new BlockPos(WIDTH / 2 + 8, Y, 0),
                    torus().nearestCopy(new BlockPos(WIDTH / 2 - 8, Y, 0), new BlockPos(-WIDTH / 2 + 8, Y, 0)),
                    "the vertex one block past the seam was not taken one world east");
        }

        @Test
        void aVertexPastTheLatticeZSeamMovesSideways() {
            assertEquals(new BlockPos(SKEW, Y, WIDTH / 2 + 8),
                    latticeTorus().nearestCopy(new BlockPos(SKEW, Y, WIDTH / 2 - 8), new BlockPos(0, Y, -WIDTH / 2 + 8)),
                    "the copy one z lap over is moved by the skew on x");
        }

        @Test
        void theShortestDeltaCrossesTheSeam() {
            assertEquals(new BlockPos(16, 0, 0),
                    torus().shortestDelta(new BlockPos(WIDTH / 2 - 8, Y, 0), new BlockPos(-WIDTH / 2 + 8, Y, 0)),
                    "the delta went the long way round");
        }

        @Test
        void aChunkPastTheSeamIsSeatedBesideTheEditedOne() {
            assertEquals(new ChunkPos(MAX_CHUNK, 0),
                    torus().nearestChunkCopy(new ChunkPos(MAX_CHUNK - 1, 0), new ChunkPos(MIN_CHUNK, 0)),
                    "the first chunk was not taken one world east of the last");
        }

        @Test
        void aFoldedChunkIsCanonical() {
            assertEquals(new ChunkPos(MIN_CHUNK, 0), cylinder().foldChunk(new ChunkPos(MAX_CHUNK, 0)),
                    "the chunk one past the last did not fold onto the first");
        }
    }

    @Nested
    class CopiesInBounds {
        private static final BlockPos BELOW_THE_Z_SEAM = new BlockPos(0, Y, -WIDTH / 2 + 8);

        @Test
        void theLatticeCopyPastTheZSeamIsTheSkewedOne() {
            assertEquals(List.of(new BlockPos(SKEW, Y, WIDTH / 2 + 8)),
                    copiesInBounds(latticeTorus(), BELOW_THE_Z_SEAM, SKEW - 8, SKEW + 8, WIDTH / 2, WIDTH / 2 + 16),
                    "the rectangle past the z seam met another copy");
        }

        @Test
        void bothMaxEdgesAreInside() {
            assertEquals(List.of(new BlockPos(SKEW, Y, WIDTH / 2 + 8)),
                    copiesInBounds(latticeTorus(), BELOW_THE_Z_SEAM, SKEW - 8, SKEW, WIDTH / 2, WIDTH / 2 + 8),
                    "a copy on the max edges was left out");
        }

        @Test
        void theUnskewedSpotPastTheSeamHoldsNoCopy() {
            assertEquals(List.of(),
                    copiesInBounds(latticeTorus(), BELOW_THE_Z_SEAM, -8, 8, WIDTH / 2, WIDTH / 2 + 16),
                    "a copy was found where a per-axis step would put it");
        }

        @Test
        void aRectangleWiderThanTheWorldMeetsEveryLap() {
            assertEquals(List.of(new BlockPos(-WIDTH, Y, 0), new BlockPos(0, Y, 0), new BlockPos(WIDTH, Y, 0)),
                    copiesInBounds(cylinder(), new BlockPos(0, Y, 0), -WIDTH, WIDTH, -8, 8).stream()
                            .sorted(Comparator.comparingInt(BlockPos::getX)).toList(),
                    "the laps inside a three-world rectangle");
        }

        @Test
        void anAcceptedCopyAnswersTrue() {
            assertTrue(torus().anyCopyInBounds(new BlockPos(-WIDTH / 2 + 8, Y, 0), WIDTH / 2, WIDTH / 2 + 16, -8, 8,
                    copy -> copy.getX() == WIDTH / 2 + 8), "the copy past the x seam was not offered");
        }
    }
}
