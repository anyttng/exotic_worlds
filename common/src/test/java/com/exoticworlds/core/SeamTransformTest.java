package com.exoticworlds.core;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class SeamTransformTest {
    private static final int SAMPLES = 200;
    private static final long SEED = 0x5EA3L;
    private static final int[] COORDS = {0, 1, -1, 7, -7, 255, -256, 4097, -4097};

    private static final int POWER_REACH = 5;

    private static final int[] CHUNK_ALIGNED_SHIFTS = {
            0,
            CoordinateConstants.CHUNK_WIDTH,
            -CoordinateConstants.CHUNK_WIDTH,
            3 * CoordinateConstants.CHUNK_WIDTH,
            -6 * CoordinateConstants.CHUNK_WIDTH,
            16 * CoordinateConstants.CHUNK_WIDTH};

    private static final int[] BLOCK_SHIFTS = {0, 1, -1, 256, -96, 95};

    private static final int BLOCK_HEIGHT = 64;
    private static final ChunkPos[] CHUNKS = {new ChunkPos(0, 0), new ChunkPos(3, -5), new ChunkPos(-7, 2)};

    private static final List<FoldOrientation> TURNS = List.of(
            FoldOrientation.IDENTITY,
            FoldOrientation.QUARTER_TURN_CLOCKWISE,
            FoldOrientation.HALF_TURN,
            FoldOrientation.QUARTER_TURN_COUNTERCLOCKWISE);

    private static List<SeamTransform> transforms() {
        return transformsWith(BLOCK_SHIFTS);
    }

    private static List<SeamTransform> chunkAlignedTransforms() {
        return transformsWith(CHUNK_ALIGNED_SHIFTS);
    }

    private static List<SeamTransform> transformsWith(int[] shifts) {
        List<SeamTransform> built = new ArrayList<>();
        for (FoldOrientation orientation : FoldOrientation.values()) {
            for (int shift : shifts) {
                built.add(transformOf(orientation, shift, -shift));
                built.add(transformOf(orientation, -shift, shift));
            }
        }

        return built;
    }

    private static SeamTransform transformOf(FoldOrientation orientation, int xShift, int zShift) {
        return new SeamTransform(
                orientation.flipsX() ? -1 : 1, orientation.flipsZ() ? -1 : 1, xShift, zShift, orientation.swapsAxes());
    }

    private static BoundingBox blocksOf(ChunkPos chunk) {
        return new BoundingBox(
                chunk.getMinBlockX(), BLOCK_HEIGHT, chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), BLOCK_HEIGHT, chunk.getMaxBlockZ());
    }

    private static boolean isChunkAligned(SeamTransform transform) {
        return transform.xShift() % CoordinateConstants.CHUNK_WIDTH == 0
                && transform.zShift() % CoordinateConstants.CHUNK_WIDTH == 0;
    }

    private static void assertChunkAligned(SeamTransform transform, String what) {
        assertEquals(0, transform.xShift() % CoordinateConstants.CHUNK_WIDTH,
                what + " carries an x shift off the chunk grid: " + transform.xShift());
        assertEquals(0, transform.zShift() % CoordinateConstants.CHUNK_WIDTH,
                what + " carries a z shift off the chunk grid: " + transform.zShift());
    }

    @Nested
    class Algebra {
        @Test
        void compositionIsAHomomorphismOnCells() {
            for (SeamTransform first : transforms()) {
                for (SeamTransform second : transforms()) {
                    SeamTransform composed = first.then(second);
                    for (int coord : COORDS) {
                        int other = -coord + 3;
                        int midX = first.applyCellX(coord, other);
                        int midZ = first.applyCellZ(coord, other);
                        assertEquals(second.applyCellX(midX, midZ), composed.applyCellX(coord, other),
                                first + " then " + second + " disagrees on cell x " + coord);
                        assertEquals(second.applyCellZ(midX, midZ), composed.applyCellZ(coord, other),
                                first + " then " + second + " disagrees on cell z " + coord);
                    }
                }
            }
        }

        @Test
        void compositionIsAHomomorphismOnCoordinates() {
            for (SeamTransform first : transforms()) {
                for (SeamTransform second : transforms()) {
                    SeamTransform composed = first.then(second);
                    for (int coord : COORDS) {
                        double x = coord + 0.5;
                        double z = -coord + 3.25;
                        double midX = first.applyX(x, z);
                        double midZ = first.applyZ(x, z);
                        assertEquals(second.applyX(midX, midZ), composed.applyX(x, z),
                                first + " then " + second + " disagrees on x " + coord);
                        assertEquals(second.applyZ(midX, midZ), composed.applyZ(x, z),
                                first + " then " + second + " disagrees on z " + coord);
                    }
                }
            }
        }

        @Test
        void theInverseUndoesTheTransformInBothForms() {
            for (SeamTransform transform : transforms()) {
                SeamTransform inverse = transform.inverse();
                for (int coord : COORDS) {
                    int other = -coord + 3;
                    int cellX = transform.applyCellX(coord, other);
                    int cellZ = transform.applyCellZ(coord, other);
                    assertEquals(coord, inverse.applyCellX(cellX, cellZ), transform + " is not undone on cell x");
                    assertEquals(other, inverse.applyCellZ(cellX, cellZ), transform + " is not undone on cell z");

                    double movedX = transform.applyX(coord + 0.5, other + 0.5);
                    double movedZ = transform.applyZ(coord + 0.5, other + 0.5);
                    assertEquals(coord + 0.5, inverse.applyX(movedX, movedZ), transform + " is not undone on x");
                    assertEquals(other + 0.5, inverse.applyZ(movedX, movedZ), transform + " is not undone on z");
                }
            }
        }

        @Test
        void powerIsRepeatedComposition() {
            for (SeamTransform transform : transforms()) {
                for (int exponent = -5; exponent <= 5; exponent++) {
                    SeamTransform repeated = SeamTransform.IDENTITY;
                    SeamTransform step = exponent < 0 ? transform.inverse() : transform;
                    for (int applied = 0; applied < Math.abs(exponent); applied++) {
                        repeated = repeated.then(step);
                    }

                    assertEquals(repeated, transform.power(exponent),
                            transform + " to the power " + exponent + " is not repeated composition");
                }
            }
        }

        @Test
        void aGlideSquaresToAPureTranslation() {
            SeamTransform glide = SeamTransform.glideX(256, 96);
            SeamTransform squared = glide.power(2);
            assertEquals(FoldOrientation.IDENTITY, squared.orientation(), "a glide squared still mirrors");
            assertEquals(new SeamTransform(1, 1, 512, 0), squared, "a glide squared is not a pure translation");
        }

        @Test
        void theOrientationFollowsTheSigns() {
            assertEquals(FoldOrientation.IDENTITY, SeamTransform.IDENTITY.orientation(), "identity mirrors");
            assertEquals(FoldOrientation.MIRROR_Z, SeamTransform.glideX(8, 0).orientation(), "a z glide is not z");
            assertEquals(FoldOrientation.MIRROR_X, SeamTransform.glideZ(0, 8).orientation(), "an x glide is not x");
            assertEquals(FoldOrientation.HALF_TURN, new SeamTransform(-1, -1, 0, 0).orientation(),
                    "a double mirror is not a half turn");
        }
    }

    @Nested
    class Guards {
        @Test
        void anAxisSignIsPlusOrMinusOne() {
            assertThrows(IllegalArgumentException.class, () -> new SeamTransform(0, 1, 0, 0), "sign 0 accepted");
            assertThrows(IllegalArgumentException.class, () -> new SeamTransform(1, 2, 0, 0), "sign 2 accepted");
        }

        @Test
        void aDeckGroupStepRefusesATurnedGenerator() {
            SeamTransform turned = transformOf(FoldOrientation.QUARTER_TURN_CLOCKWISE, 256, 0);
            assertThrows(IllegalArgumentException.class, () -> new DeckGroupLattice.Step(turned, true),
                    "a turned generator reached the per-axis reduction");
        }

        @Test
        void aShiftTooLargeToRepresentIsRefusedRatherThanWrapped() {
            SeamTransform wide = SeamTransform.translation(Integer.MAX_VALUE / 2, 0);
            assertThrows(ArithmeticException.class, () -> wide.power(5), "an overflowing shift was wrapped");
        }
    }

    @Nested
    class QuarterTurns {
        @Test
        void theOrientationIsTheOneTheTransformWasBuiltFrom() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertEquals(orientation, transformOf(orientation, 32, -48).orientation(),
                        orientation + " is not read back");
            }
        }

        @Test
        void fourQuarterTurnsAboutAPointAreTheIdentity() {
            for (SeamTransform transform : transforms()) {
                if (transform.swapsAxes() && transform.orientation().preservesHandedness()) {
                    assertEquals(SeamTransform.IDENTITY, transform.power(4), transform + " four times leaves a move");
                    assertEquals(transform.inverse(), transform.power(-1), transform + " has another inverse");
                }
            }
        }

        @Test
        void aTurnedChunkIsAChunk() {
            for (FoldOrientation turn : TURNS) {
                for (int shift : CHUNK_ALIGNED_SHIFTS) {
                    SeamTransform transform = transformOf(turn, shift, -shift);
                    DeckTransformation deck = new DeckTransformation(transform);
                    for (ChunkPos chunk : CHUNKS) {
                        assertEquals(blocksOf(deck.apply(chunk)), transform.apply(blocksOf(chunk)),
                                transform + " does not carry the blocks of " + chunk + " onto one chunk");
                    }
                }
            }
        }

        @Test
        void aTurnedChunkCornerIsAChunkCorner() {
            for (FoldOrientation turn : TURNS) {
                SeamTransform transform = transformOf(turn, 3 * CoordinateConstants.CHUNK_WIDTH, 0);
                for (ChunkPos chunk : CHUNKS) {
                    assertCornersStayCorners(transform, chunk);
                }
            }
        }

        private static void assertCornersStayCorners(SeamTransform transform, ChunkPos chunk) {
            BoundingBox image = transform.apply(blocksOf(chunk));
            for (int x : new int[] {chunk.getMinBlockX(), chunk.getMaxBlockX()}) {
                for (int z : new int[] {chunk.getMinBlockZ(), chunk.getMaxBlockZ()}) {
                    BlockPos corner = transform.apply(new BlockPos(x, BLOCK_HEIGHT, z));
                    assertTrue(corner.getX() == image.minX() || corner.getX() == image.maxX(),
                            transform + " puts a corner of " + chunk + " inside the chunk on x");
                    assertTrue(corner.getZ() == image.minZ() || corner.getZ() == image.maxZ(),
                            transform + " puts a corner of " + chunk + " inside the chunk on z");
                }
            }
        }

        @Test
        void aBlockLandsOnTheBlockHoldingItsCenter() {
            for (SeamTransform transform : transforms()) {
                for (int coord : COORDS) {
                    BlockPos block = new BlockPos(coord, BLOCK_HEIGHT, -coord + 3);
                    assertEquals(BlockPos.containing(transform.apply(Vec3.atCenterOf(block))), transform.apply(block),
                            transform + " carries " + block + " off the block holding its center");
                    assertEquals(new AABB(transform.apply(block)), transform.apply(new AABB(block)),
                            transform + " carries the box of " + block + " off its block");
                }
            }
        }
    }

    @Nested
    class CellsAgainstCoordinates {
        @Test
        void aCellCenterTransformsToTheTransformedCellCenter() {
            Random random = new Random(SEED);
            for (SeamTransform transform : transforms()) {
                for (int sample = 0; sample < SAMPLES; sample++) {
                    int cellX = random.nextInt(20000) - 10000;
                    int cellZ = random.nextInt(20000) - 10000;
                    assertEquals(transform.applyCellX(cellX, cellZ) + 0.5, transform.applyX(cellX + 0.5, cellZ + 0.5),
                            transform + " puts the cell center elsewhere on x");
                    assertEquals(transform.applyCellZ(cellX, cellZ) + 0.5, transform.applyZ(cellX + 0.5, cellZ + 0.5),
                            transform + " puts the cell center elsewhere on z");
                }
            }
        }

        @Test
        void aMirrorShiftsCellsByOneAgainstCoordinates() {
            SeamTransform mirror = SeamTransform.glideX(0, 0);
            assertEquals(-1, mirror.applyCellZ(0, 0), "the cell at 0 does not reflect onto -1");
            assertEquals(0.0, mirror.applyZ(0.0, 0.0), "the coordinate 0 does not reflect onto itself");
            assertTrue(mirror.applyCellZ(0, 5) == -6 && mirror.applyZ(0.0, 5.0) == -5.0,
                    "the cell form and the coordinate form of a mirror are the same map");
        }
    }

    @Nested
    class ShiftGranularity {
        @Test
        void chunkAlignedShiftsSurviveCompositionInversionAndPower() {
            for (SeamTransform transform : chunkAlignedTransforms()) {
                assertChunkAligned(transform.inverse(), "the inverse of " + transform);
                for (int exponent = -POWER_REACH; exponent <= POWER_REACH; exponent++) {
                    assertChunkAligned(transform.power(exponent), transform + " to the power " + exponent);
                }

                for (SeamTransform next : chunkAlignedTransforms()) {
                    assertChunkAligned(transform.then(next), transform + " then " + next);
                }
            }
        }

        @Test
        void aShiftOffTheChunkGridStaysOffItThroughEveryComposition() {
            SeamTransform offGrid = SeamTransform.translation(CoordinateConstants.CHUNK_WIDTH / 2, 0);
            assertNotEquals(0, offGrid.xShift() % CoordinateConstants.CHUNK_WIDTH,
                    "half a chunk counted as chunk-aligned");

            for (SeamTransform aligned : chunkAlignedTransforms()) {
                assertFalse(isChunkAligned(offGrid.then(aligned)),
                        "an off-grid shift then " + aligned + " came back aligned");
                assertFalse(isChunkAligned(aligned.then(offGrid)),
                        aligned + " then an off-grid shift came back aligned");
            }
        }
    }
}
