package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

class DhLatticeFoldTest {
    private static final byte LEAF = 6;
    private static final byte SECTION_128 = 7;
    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;
    private static final int LEAVES_PER_WORLD = WIDTH_BLOCKS / 64;
    private static final int LEAF_SKEW_CHUNKS = 4;
    private static final int CHUNK_SKEW_CHUNKS = 1;
    private static final int SECTION_128_SKEW_CHUNKS = 8;
    private static final int LAPS = 3;

    private static DhLattice lattice(int skewChunks) {
        return DhLattice.of(WorldFolds.of(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidths(WIDTH_CHUNKS, WIDTH_CHUNKS), skewChunks)));
    }

    private static int minChunk() {
        return lattice(LEAF_SKEW_CHUNKS).shape().minChunk(Direction.Axis.X);
    }

    @Nested
    class TheSublatticeTheSectionGridKeeps {
        @Test
        void aSkewOfWholeLeavesKeepsOneLapOnBothAxes() {
            assertEquals(new DhFold.Period(WIDTH_BLOCKS, WIDTH_BLOCKS, 64), DhFold.period(lattice(LEAF_SKEW_CHUNKS), LEAF));
        }

        @Test
        void aSkewOfOneChunkTakesFourZLapsToLandOnTheLeafGrid() {
            assertEquals(new DhFold.Period(WIDTH_BLOCKS, 4L * WIDTH_BLOCKS, 64),
                    DhFold.period(lattice(CHUNK_SKEW_CHUNKS), LEAF));
        }

        @Test
        void theSkewLimitsTheExactLevelLikeAWidthDoes() {
            assertEquals(6, DhFold.maxExactDetailLevel(lattice(LEAF_SKEW_CHUNKS)));
            assertEquals(4, DhFold.maxExactDetailLevel(lattice(CHUNK_SKEW_CHUNKS)));
            assertEquals(LEAF, DhFold.maxRenderableDetailLevel(lattice(CHUNK_SKEW_CHUNKS), LEAF));
        }
    }

    @Nested
    class KeysFoldThroughTheSkew {
        private final DhLattice lattice = lattice(LEAF_SKEW_CHUNKS);
        private final int origin = minChunk() * 16 / 64;

        @Test
        void aLeafOneZLapOnLandsWhereTheSkewMovesIt() {
            int x = this.origin + 3;
            int z = this.origin + LEAVES_PER_WORLD;
            assertEquals(this.origin + 2, DhFold.foldSectionX(this.lattice, LEAF, x, z));
            assertEquals(this.origin, DhFold.foldSectionZ(this.lattice, LEAF, z));
        }

        @Test
        void everyLatticeCopyOfALeafSharesItsKeyInsideTheWorld() {
            for (int x = this.origin; x < this.origin + LEAVES_PER_WORLD; x++) {
                for (int z = this.origin; z < this.origin + LEAVES_PER_WORLD; z++) {
                    assertEquals(x, DhFold.foldSectionX(this.lattice, LEAF, x, z));
                    assertEquals(z, DhFold.foldSectionZ(this.lattice, LEAF, z));
                    for (int i = -LAPS; i <= LAPS; i++) {
                        for (int j = -LAPS; j <= LAPS; j++) {
                            int copyX = x + i * LEAVES_PER_WORLD + j;
                            int copyZ = z + j * LEAVES_PER_WORLD;
                            String where = "leaf " + x + "," + z + " copy " + i + "," + j;
                            assertEquals(x, DhFold.foldSectionX(this.lattice, LEAF, copyX, copyZ), where);
                            assertEquals(z, DhFold.foldSectionZ(this.lattice, LEAF, copyZ), where);
                        }
                    }
                }
            }
        }

        @Test
        void aSkewOffTheLeafGridStillFoldsOntoTheGrid() {
            DhLattice chunkSkew = lattice(CHUNK_SKEW_CHUNKS);
            int z = this.origin + 4 * LEAVES_PER_WORLD;
            assertEquals(this.origin + 4, DhFold.foldSectionX(chunkSkew, LEAF, this.origin + 5, z));
            assertEquals(this.origin, DhFold.foldSectionZ(chunkSkew, LEAF, z));
            assertEquals(this.origin + 5,
                    DhFold.foldSectionX(chunkSkew, LEAF, this.origin + 5, this.origin + LEAVES_PER_WORLD));
        }

        @Test
        void theChunkFoldLandsInTheFoldedLeafOfItsRawLeaf() {
            for (DhLattice shape : new DhLattice[] {this.lattice, lattice(CHUNK_SKEW_CHUNKS)}) {
                for (int chunkX = -300; chunkX <= 300; chunkX += 7) {
                    for (int chunkZ = -300; chunkZ <= 300; chunkZ += 11) {
                        int foldedX = DhFold.foldChunkX(shape, LEAF, chunkX, chunkZ);
                        int foldedZ = DhFold.foldChunkZ(shape, LEAF, chunkZ);
                        String where = "chunk " + chunkX + "," + chunkZ;
                        assertEquals(chunkX & 3, foldedX & 3, where);
                        assertEquals(chunkZ & 3, foldedZ & 3, where);
                        int leafX = Math.floorDiv(chunkX, 4);
                        int leafZ = Math.floorDiv(chunkZ, 4);
                        assertEquals(DhFold.foldSectionX(shape, LEAF, leafX, leafZ),
                                DhFold.foldSectionX(shape, LEAF, Math.floorDiv(foldedX, 4), Math.floorDiv(foldedZ, 4)),
                                where);
                        assertEquals(DhFold.foldSectionZ(shape, LEAF, leafZ),
                                DhFold.foldSectionZ(shape, LEAF, Math.floorDiv(foldedZ, 4)), where);
                    }
                }
            }
        }

        @Test
        void aChunkKeyPastTheZSeamFoldsLikeTheShapeFoldsItsChunk() {
            for (int chunkX = -100; chunkX <= 100; chunkX += 13) {
                for (int chunkZ = -100; chunkZ <= 100; chunkZ += 17) {
                    ChunkPos raw = new ChunkPos(chunkX, chunkZ);
                    assertEquals(this.lattice.shape().fold(raw), DhKeys.foldChunk(this.lattice, raw),
                            "chunk " + chunkX + "," + chunkZ);
                }
            }
        }

        @Test
        void aSectionKeyPastTheZSeamCarriesTheSkew() {
            long folded = DhKeys.foldSection(this.lattice,
                    DhSectionPos.encode(LEAF, this.origin + 3, this.origin + LEAVES_PER_WORLD));
            assertEquals(this.origin + 2, DhSectionPos.getX(folded));
            assertEquals(this.origin, DhSectionPos.getZ(folded));
        }

        @Test
        void aBeaconKeyFoldsThroughTheShapesOwnFold() {
            int min = minChunk() * 16;
            DhBlockPos folded = DhKeys.foldBlock(this.lattice, new DhBlockPos(min + 100, 64, min + WIDTH_BLOCKS + 10));
            assertEquals(min + 36, folded.getX());
            assertEquals(64, folded.getY());
            assertEquals(min + 10, folded.getZ());
        }

        @Test
        void aBeaconKeyInsideTheWorldComesBackItself() {
            int min = minChunk() * 16;
            DhBlockPos inside = new DhBlockPos(min + 100, 64, min + 10);
            assertSame(inside, DhKeys.foldBlock(this.lattice, inside));
        }
    }

    @Nested
    class CopiesAndCompletenessThroughTheSkew {
        private final DhLattice lattice = lattice(LEAF_SKEW_CHUNKS);
        private final int origin = minChunk() * 16 / 64;

        @Test
        void aSectionPastTheZSeamContainsTheLeavesTheSkewMovesUnderIt() {
            int x = this.origin / 2 + 1;
            int z = this.origin / 2 + LEAVES_PER_WORLD / 2;
            assertTrue(DhFold.containsACopy(this.lattice, SECTION_128, x, z, LEAF, this.origin + 1, this.origin));
            assertTrue(DhFold.containsACopy(this.lattice, SECTION_128, x, z, LEAF, this.origin + 2, this.origin));
            assertFalse(DhFold.containsACopy(this.lattice, SECTION_128, x, z, LEAF, this.origin + 3, this.origin));
            assertFalse(DhFold.containsACopy(this.lattice, SECTION_128, x, z, LEAF, this.origin, this.origin));
        }

        @Test
        void aCoarseSectionOneZLapOnIsCompleteOnlyWhereTheSkewKeepsItsGrid() {
            int x = this.origin / 2 + 1;
            int z = this.origin / 2 + LEAVES_PER_WORLD / 2;
            assertFalse(DhFold.isCompleteSection(this.lattice, LEAF, SECTION_128, x, z));
            assertTrue(DhFold.isCompleteSection(lattice(SECTION_128_SKEW_CHUNKS), LEAF, SECTION_128, x, z));
        }
    }

    @Nested
    class TheWindowDrawsEachPositionOnceThroughTheSkew {
        private final DhLattice lattice = lattice(LEAF_SKEW_CHUNKS);
        private final int origin = minChunk() * 16 / 64;

        @Test
        void everyCanonicalLeafHasOneDrawnCopyAndTheReloadLandsOnIt() {
            int min = minChunk() * 16;
            for (int refX = min - 300; refX <= min + WIDTH_BLOCKS + 300; refX += 173) {
                for (int refZ = min - 300; refZ <= min + WIDTH_BLOCKS + 300; refZ += 191) {
                    for (int x = this.origin; x < this.origin + LEAVES_PER_WORLD; x += 3) {
                        for (int z = this.origin; z < this.origin + LEAVES_PER_WORLD; z += 5) {
                            assertOneDrawnCopy(refX, refZ, x, z);
                        }
                    }
                }
            }
        }

        @Test
        void everyBeaconHasOneDrawnBeamAmongTheCopiesInDhsWholeRadius() {
            int min = minChunk() * 16;
            int radiusLaps = 8;
            for (DhLattice shape : new DhLattice[] {this.lattice, lattice(0)}) {
                for (int refX = min - 300; refX <= min + WIDTH_BLOCKS + 300; refX += 211) {
                    for (int refZ = min - 300; refZ <= min + WIDTH_BLOCKS + 300; refZ += 223) {
                        for (int beaconX = min; beaconX < min + WIDTH_BLOCKS; beaconX += 97) {
                            for (int beaconZ = min; beaconZ < min + WIDTH_BLOCKS; beaconZ += 89) {
                                int drawn = 0;
                                for (int i = -radiusLaps; i <= radiusLaps; i++) {
                                    for (int j = -radiusLaps; j <= radiusLaps; j++) {
                                        int skew = shape.skewBlocks() * j;
                                        DhBlockPos copy = new DhBlockPos(beaconX + i * WIDTH_BLOCKS + skew, 64,
                                                beaconZ + j * WIDTH_BLOCKS);
                                        if (DhKeys.isNearestBeam(shape, refX, refZ, copy)) {
                                            drawn++;
                                        }
                                    }
                                }

                                assertEquals(1, drawn, "skew " + shape.skewBlocks() + " ref " + refX + "," + refZ
                                        + " beacon " + beaconX + "," + beaconZ);
                            }
                        }
                    }
                }
            }
        }

        private void assertOneDrawnCopy(int refX, int refZ, int x, int z) {
            long kept = DhKeys.nearestSection(this.lattice, refX, refZ, DhSectionPos.encode(LEAF, x, z));
            int drawn = 0;
            for (int i = -LAPS; i <= LAPS; i++) {
                for (int j = -LAPS; j <= LAPS; j++) {
                    long copy = DhSectionPos.encode(LEAF, x + i * LEAVES_PER_WORLD + j, z + j * LEAVES_PER_WORLD);
                    String where = "ref " + refX + "," + refZ + " leaf " + x + "," + z + " copy " + i + "," + j;
                    if (DhKeys.isNearestCopy(this.lattice, refX, refZ, copy)) {
                        drawn++;
                        assertEquals(kept, copy, where);
                    }

                    assertEquals(kept, DhKeys.nearestSection(this.lattice, refX, refZ, copy), where);
                }
            }

            assertEquals(1, drawn, "ref " + refX + "," + refZ + " leaf " + x + "," + z);
        }
    }

    @Nested
    class TheGenerationQueueMeasuresThroughTheSkew {
        private final DhLattice lattice = lattice(LEAF_SKEW_CHUNKS);

        @Test
        void aPointOneZLapOnIsBesideItsSkewedCopy() {
            int min = minChunk() * 16;
            assertEquals(34, DhFold.seamChebyshevDistance(this.lattice, min + 100, min + WIDTH_BLOCKS - 24,
                    min + 36, min + 10));
        }

        @Test
        void theDistanceIsTheShortestOverEveryLatticeCopy() {
            for (int toX = -700; toX <= 700; toX += 97) {
                for (int toZ = -1500; toZ <= 1500; toZ += 131) {
                    long nearest = Long.MAX_VALUE;
                    for (int i = -LAPS; i <= LAPS; i++) {
                        for (int j = -LAPS; j <= LAPS; j++) {
                            long dx = Math.abs(toX + (long) i * WIDTH_BLOCKS + j * 64L);
                            long dz = Math.abs(toZ + (long) j * WIDTH_BLOCKS);
                            nearest = Math.min(nearest, Math.max(dx, dz));
                        }
                    }

                    assertEquals(nearest, DhFold.seamChebyshevDistance(this.lattice, 0, 0, toX, toZ),
                            "to " + toX + "," + toZ);
                }
            }
        }
    }
}
