package com.exoticworlds.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.dto.ChunkHashDTO;

import net.minecraft.core.Direction;

class DhFoldTest {
    private static final byte LEAF = 6;
    private static final byte CHUNK_16 = 8;
    private static final byte WORLD = 10;
    private static final byte SNAP = 6;

    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;

    private static DhLattice torus(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return DhLattice.of(
                WorldFolds.of(FlatShape.torus(new WorldLoopBounds(looped, looped))));
    }

    private static DhLattice cylinder(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return DhLattice.of(WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE))));
    }

    @Nested
    class SectionsFoldByTheirMinCorner {
        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);

        @Test
        void aLeafPastTheSeamLandsOnTheFirstLeafOfTheWorld() {
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertEquals(0, DhFold.foldSectionX(lattice, LEAF, sectionsPerWorld, 0));
            assertEquals(sectionsPerWorld - 1, DhFold.foldSectionX(lattice, LEAF, -1, 0));
            assertEquals(3, DhFold.foldSectionZ(lattice, LEAF, 3 + 2 * sectionsPerWorld));
        }

        @Test
        void aSectionInsideTheWorldIsItsOwnKey() {
            assertEquals(5, DhFold.foldSectionX(lattice, LEAF, 5, 0));
            assertEquals(3, DhFold.foldSectionX(lattice, CHUNK_16, 3, 0));
            assertEquals(0, DhFold.foldSectionX(lattice, WORLD, 0, 0));
        }

        @Test
        void aSectionAsWideAsTheWorldFoldsOntoTheOne() {
            assertEquals(0, DhFold.foldSectionX(lattice, WORLD, 1, 0));
            assertEquals(0, DhFold.foldSectionX(lattice, WORLD, -1, 0));
        }

        @Test
        void theUnboundedAxisOfACylinderPassesThrough() {
            DhLattice cylinder = cylinder(0, WIDTH_CHUNKS);
            assertEquals(-7, DhFold.foldSectionZ(cylinder, LEAF, -7));
            assertEquals(0, DhFold.foldSectionX(cylinder, LEAF, WIDTH_BLOCKS / 64, 0));
        }

        @Test
        void aWorldNotStartingAtZeroFoldsIntoItsOwnSpan() {
            DhLattice shifted = torus(-32, 32);
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertEquals(-sectionsPerWorld / 2, DhFold.foldSectionX(shifted, LEAF, sectionsPerWorld / 2, 0));
            assertEquals(sectionsPerWorld / 2 - 1, DhFold.foldSectionX(shifted, LEAF, -sectionsPerWorld / 2 - 1, 0));
        }
    }

    @Nested
    class ExactnessIsDivisibility {
        @Test
        void aPowerOfTwoWorldFoldsEveryLevelUpToItsOwnWidth() {
            assertEquals(WORLD, DhFold.maxExactDetailLevel(torus(0, WIDTH_CHUNKS)));
        }

        @Test
        void anOddWidthStopsAtTheLargestPowerOfTwoDividingIt() {
            assertEquals(LEAF, DhFold.maxExactDetailLevel(torus(0, 100)));
        }

        @Test
        void theUnboundedAxisNeverLimits() {
            assertEquals(WORLD, DhFold.maxExactDetailLevel(cylinder(0, WIDTH_CHUNKS)));
        }

        @Test
        void aShapeLoopingOnNoAxisNeverLimits() {
            assertEquals(Byte.MAX_VALUE, DhFold.maxExactDetailLevel(DhLattice.of(WorldFolds.NOOP)));
        }
    }

    @Nested
    class TheWidestSectionThatMayRender {
        @Test
        void aWorldOf1024BlocksRendersSectionsOf1024Blocks() {
            assertEquals(WORLD, DhFold.maxRenderableDetailLevel(torus(0, WIDTH_CHUNKS), LEAF));
        }

        @Test
        void aWorldOf1664BlocksRendersSectionsOf128Blocks() {
            assertEquals(7, DhFold.maxRenderableDetailLevel(torus(0, 104), LEAF));
        }

        @Test
        void aWorldOf1600BlocksRendersOnlyTheLeaf() {
            assertEquals(LEAF, DhFold.maxRenderableDetailLevel(torus(0, 100), LEAF));
        }

        @Test
        void aWorldOf1616BlocksNoSectionDividesStillRendersTheLeaf() {
            assertEquals(LEAF, DhFold.maxRenderableDetailLevel(torus(0, 101), LEAF));
        }

        @Test
        void aWorldOf800BlocksNoSectionDividesStillRendersTheLeaf() {
            assertEquals(LEAF, DhFold.maxRenderableDetailLevel(torus(0, 50), LEAF));
        }
    }

    @Nested
    class AlignmentCountsAsMuchAsWidth {
        @Test
        void aWorldWhoseOriginIsOffTheGridStopsAtTheLevelItsOriginAllows() {
            assertEquals(LEAF, DhFold.maxExactDetailLevel(torus(-52, 52)));
        }

        @Test
        void theSameWidthStartingOnTheGridReachesTheLevelItsWidthAllows() {
            assertEquals(7, DhFold.maxExactDetailLevel(torus(0, 104)));
        }

        @Test
        void aWorldStartingAtZeroIsLimitedByItsWidthAlone() {
            assertEquals(WORLD, DhFold.maxExactDetailLevel(torus(0, WIDTH_CHUNKS)));
        }
    }

    @Nested
    class KeysFoldByThePeriodOfTheirLevel {
        private static final int ODD_CHUNKS = 101;
        private static final int ODD_BLOCKS = ODD_CHUNKS * 16;
        private static final int ODD_LEAF_SECTIONS = 101;

        private final DhLattice odd = torus(0, ODD_CHUNKS);

        @Test
        void thePeriodIsTheSmallestWholeNumberOfLapsTheSectionGridDivides() {
            assertEquals(4 * ODD_BLOCKS, DhFold.period(odd, LEAF).xBlocks());
            assertEquals(2 * 800, DhFold.period(torus(0, 50), LEAF).xBlocks());
            assertEquals(1664, DhFold.period(torus(0, 104), LEAF).xBlocks());
            assertEquals(ODD_LEAF_SECTIONS, DhFold.periodSections(odd.shape(), Direction.Axis.X, LEAF));
            assertEquals(25, DhFold.periodSections(torus(0, 50).shape(), Direction.Axis.X, LEAF));
            assertEquals(26, DhFold.periodSections(torus(0, 104).shape(), Direction.Axis.X, LEAF));
        }

        @Test
        void aLapThatLandsOffTheSectionGridKeepsItsOwnKeys() {
            assertEquals(26, DhFold.foldSectionX(odd, LEAF, 26, 0));
            assertEquals(75, DhFold.foldSectionX(odd, LEAF, 75, 0));
            assertEquals(100, DhFold.foldSectionX(odd, LEAF, -1, 0));
            assertEquals(13, DhFold.foldSectionX(torus(0, 50), LEAF, 13, 0));
        }

        @Test
        void theLapThatLandsOnTheGridFoldsOntoTheFirst() {
            assertEquals(0, DhFold.foldSectionX(odd, LEAF, ODD_LEAF_SECTIONS, 0));
            assertEquals(5, DhFold.foldSectionX(odd, LEAF, 5 + 2 * ODD_LEAF_SECTIONS, 0));
            assertEquals(0, DhFold.foldSectionX(torus(0, 50), LEAF, 25, 0));
            assertEquals(0, DhFold.foldSectionX(torus(0, 104), LEAF, 26, 0));
        }

        @Test
        void twoSectionsShareAKeyOnlyAWholeNumberOfWorldWidthsApart() {
            for (int section = -300; section <= 300; section++) {
                int key = DhFold.foldSectionX(odd, LEAF, section, 0);
                assertTrue(0 <= key && key < ODD_LEAF_SECTIONS, "section " + section);
                assertEquals(key, DhFold.foldSectionX(odd, LEAF, section + ODD_LEAF_SECTIONS, 0));
                for (int apart = 1; apart < ODD_LEAF_SECTIONS; apart++) {
                    assertNotEquals(key, DhFold.foldSectionX(odd, LEAF, section + apart, 0),
                            "section " + section + " apart " + apart);
                }
            }
        }

        @Test
        void theChunkFoldLandsInTheFoldedSectionOfItsRawSection() {
            for (DhLattice lattice : new DhLattice[] {odd, torus(-50, 50), torus(-16, 16)}) {
                for (int chunk = -500; chunk <= 500; chunk++) {
                    int folded = DhFold.foldChunkX(lattice, LEAF, chunk, 0);
                    assertEquals(chunk & 3, folded & 3, "chunk " + chunk);
                    assertEquals(DhFold.foldSectionX(lattice, LEAF, Math.floorDiv(chunk, 4), 0),
                            DhFold.foldSectionX(lattice, LEAF, Math.floorDiv(folded, 4), 0),
                            "chunk " + chunk);
                }
            }
        }

        @Test
        void theUnboundedAxisOfACylinderHasNoPeriodToFoldBy() {
            DhLattice cylinder = cylinder(0, ODD_CHUNKS);
            assertEquals(9999, DhFold.foldSectionZ(cylinder, LEAF, 9999));
            assertEquals(9999, DhFold.foldChunkZ(cylinder, LEAF, 9999));
            assertEquals(26, DhFold.foldSectionX(cylinder, LEAF, 26, 0));
            assertEquals(0, DhFold.foldSectionX(cylinder, LEAF, ODD_LEAF_SECTIONS, 0));
        }
    }

    @Nested
    class KeysAboveTheExactLevelFoldByAWiderPeriod {
        private static final byte SECTION_128 = 7;
        private static final byte SECTION_512 = 9;

        private final DhLattice tiny = torus(-16, 16);

        @Test
        void aWorldCentredOnZeroIsExactUpToItsHalfWidth() {
            assertEquals(CHUNK_16, DhFold.maxExactDetailLevel(tiny));
        }

        @Test
        void aSectionAsWideAsTheWorldFoldsOntoOneKeyWhateverItsLap() {
            assertEquals(512, DhFold.period(tiny, SECTION_512).xBlocks());
            assertEquals(0, DhFold.foldSectionX(tiny, SECTION_512, -1, 0));
            assertEquals(0, DhFold.foldSectionX(tiny, SECTION_512, 0, 0));
            assertEquals(0, DhFold.foldSectionX(tiny, SECTION_512, 7, 0));
        }

        @Test
        void aSectionAtTheExactLevelStillFolds() {
            assertEquals(0, DhFold.foldSectionX(tiny, CHUNK_16, -2, 0));
            assertEquals(-1, DhFold.foldSectionX(tiny, CHUNK_16, -1, 0));
        }

        @Test
        void aWorldTheSectionDoesNotDivideFoldsByWholeLapsOfIt() {
            DhLattice odd = torus(0, 101);
            assertEquals(8 * 1616, DhFold.period(odd, SECTION_128).xBlocks());
            assertEquals(101, DhFold.periodSections(odd.shape(), Direction.Axis.X, SECTION_128));
            assertEquals(50, DhFold.foldSectionX(odd, SECTION_128, 50 + 101, 0));
            assertEquals(2 * 1600, DhFold.period(torus(-50, 50), SECTION_128).xBlocks());
        }
    }

    @Nested
    class ASectionIsCompleteOrItIsNotDrawn {
        private static final byte SECTION_128 = 7;
        private static final byte SECTION_256 = 8;

        private final DhLattice odd = torus(0, 101);

        @Test
        void aLeafIsCompleteWhereverItSits() {
            assertTrue(DhFold.isCompleteSection(odd, LEAF, LEAF, 25, 25));
            assertTrue(DhFold.isCompleteSection(odd, LEAF, LEAF, 26, 26));
            assertTrue(DhFold.isCompleteSection(odd, LEAF, LEAF, -1, 300));
        }

        @Test
        void aCoarseSectionInsideTheFirstLapIsComplete() {
            assertTrue(DhFold.isCompleteSection(odd, LEAF, SECTION_256, 1, 1));
            assertTrue(DhFold.isCompleteSection(odd, LEAF, SECTION_256, 5, 0));
            assertTrue(DhFold.isCompleteSection(torus(0, 104), LEAF, SECTION_256, 5, 5));
        }

        @Test
        void aCoarseSectionHangingOverTheWorldEdgeIsNot() {
            assertFalse(DhFold.foldedSpanInsideTheWorld(odd, SECTION_256, 6, 0));
            assertFalse(DhFold.isCompleteSection(odd, LEAF, SECTION_256, 6, 1));
            assertFalse(DhFold.foldedSpanInsideTheWorld(torus(0, 104), SECTION_256, 6, 0));
        }

        @Test
        void aCoarseSectionInAFarLapIsCompleteOnlyWhereItFoldsIntoTheFirst() {
            assertFalse(DhFold.foldedSpanInsideTheWorld(odd, SECTION_256, 7, 0));
            assertFalse(DhFold.foldedSpanInsideTheWorld(odd, SECTION_256, 6 + 101, 0));
            assertTrue(DhFold.foldedSpanInsideTheWorld(odd, SECTION_256, 1 + 101, 0));
            assertTrue(DhFold.foldedSpanInsideTheWorld(torus(0, 104), SECTION_256, 13, 0));
        }

        @Test
        void aStraddlerOfAnOffGridWorldEdgeIsIncompleteOnBothSides() {
            DhLattice shifted = torus(-50, 50);
            assertFalse(DhFold.foldedSpanInsideTheWorld(shifted, SECTION_128, 6, 0));
            assertFalse(DhFold.foldedSpanInsideTheWorld(shifted, SECTION_128, -7, 0));
            assertTrue(DhFold.foldedSpanInsideTheWorld(shifted, SECTION_128, 0, 0));
            assertTrue(DhFold.foldedSpanInsideTheWorld(shifted, SECTION_128, -6, 0));
        }

        @Test
        void theUnboundedAxisOfACylinderIsAlwaysComplete() {
            assertTrue(DhFold.foldedSpanInsideTheWorld(cylinder(0, 101), SECTION_256, 1, 9999));
            assertTrue(DhFold.isCompleteSection(cylinder(0, 101), LEAF, SECTION_256, 1, 9999));
        }
    }

    @Nested
    class ASectionContainsACopyOfAGenerationPosition {
        private static final byte SECTION_128 = 7;

        private final DhLattice odd = torus(0, 101);

        @Test
        void theCopyInsideTheSectionIsFoundWhicheverLapTheSectionIsIn() {
            assertTrue(DhFold.containsACopy(odd, SECTION_128, 114, 0, LEAF, 26, 0));
            assertTrue(DhFold.containsACopy(odd, SECTION_128, 114, 0, LEAF, 27, 0));
            assertFalse(DhFold.containsACopy(odd, SECTION_128, 114, 0, LEAF, 28, 0));
            assertFalse(DhFold.containsACopy(odd, SECTION_128, 114, 0, LEAF, 25, 0));
        }

        @Test
        void aSectionContainsItsOwnCopiesAndNoNeighbour() {
            assertTrue(DhFold.containsACopy(odd, LEAF, 5, 0, LEAF, 5 + 3 * 101, 0));
            assertFalse(DhFold.containsACopy(odd, LEAF, 5, 0, LEAF, 6, 0));
        }

        @Test
        void theUnboundedAxisOfACylinderContainsOnlyWhatLiesInside() {
            DhLattice cylinder = cylinder(0, 101);
            assertTrue(DhFold.containsACopy(cylinder, SECTION_128, 0, 3, LEAF, 0, 7));
            assertFalse(DhFold.containsACopy(cylinder, SECTION_128, 0, 3, LEAF, 0, 7 + 101));
        }
    }

    @Nested
    class TheCapReachesDhInItsOwnUnit {
        @Test
        void aWorldOf1024BlocksAllowsSectionsOf1024Blocks() {
            assertEquals(4, DhFold.maxExpectedDetailLevel(torus(0, WIDTH_CHUNKS), LEAF));
        }

        @Test
        void aWorldOf1664BlocksAllowsSectionsOf128Blocks() {
            assertEquals(1, DhFold.maxExpectedDetailLevel(torus(0, 104), LEAF));
        }

        @Test
        void aWorldOf1600BlocksAllowsOnlyTheLeaf() {
            assertEquals(0, DhFold.maxExpectedDetailLevel(torus(0, 100), LEAF));
        }

        @Test
        void aWorldNoSectionDividesStopsAtTheLeafRatherThanBelowIt() {
            assertEquals(0, DhFold.maxExpectedDetailLevel(torus(0, 101), LEAF));
        }
    }

    @Nested
    class TheNearestSectionFollowsTheReference {
        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);

        @Test
        void aCanonicalSectionBehindTheSeamIsSeatedBesideTheReference() {
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertEquals(-1, DhFold.nearestSection(lattice, SNAP, LEAF, 10, 0, sectionsPerWorld - 1, 0).x());
            assertEquals(sectionsPerWorld, DhFold.nearestSection(lattice, SNAP, LEAF, WIDTH_BLOCKS - 10, 0, 0, 0).x());
        }

        @Test
        void aSectionAlreadyNearTheReferenceStays() {
            assertEquals(4, DhFold.nearestSection(lattice, SNAP, LEAF, 300, 0, 4, 0).x());
        }

        @Test
        void aSectionAboveTheExactLevelIsSeatedByItsOwnPeriod() {
            DhLattice tiny = torus(-16, 16);
            assertEquals(0, DhFold.nearestSection(tiny, SNAP, (byte) 9, 0, 0, -1, 0).x());
            assertEquals(-1, DhFold.nearestSection(tiny, SNAP, (byte) 9, -100, 0, -1, 0).x());
        }

        @Test
        void aKeyOfAWiderPeriodIsSeatedOnTheCopyNearestTheReference() {
            DhLattice odd = torus(0, 101);
            int ref = 4 * 1616 + 10;
            assertEquals(106, DhFold.nearestSection(odd, SNAP, LEAF, ref, 0, 5, 0).x());
            assertEquals(106, DhFold.nearestSection(odd, SNAP, LEAF, ref, 0, 106 + 101, 0).x());
        }
    }

    @Nested
    class OnlyTheNearestCopyIsDrawn {
        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionJustAcrossTheSeamIsDrawnAndItsFarCopyIsNot() {
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertTrue(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, LEAF, 60, 4));
            assertFalse(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, LEAF, 60, 4 - sectionsPerWorld));
        }

        @Test
        void theAntipodeTieDrawsThePositiveSide() {
            assertTrue(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, LEAF, 32, 8));
            assertFalse(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, LEAF, 32, -8));
        }

        @Test
        void aSectionAsWideAsTheWorldStraddlesTheWindowAndNeverDrawsWhole() {
            DhLattice tiny = torus(-16, 16);
            assertFalse(DhFold.isNearestSection(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, 0));
            assertFalse(DhFold.isNearestSection(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, -1));
            assertTrue(DhFold.overlapsNearestWindow(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, 0));
            assertTrue(DhFold.overlapsNearestWindow(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, -1));
        }

        @Test
        void theUnboundedAxisOfACylinderNeverCulls() {
            DhLattice cylinder = cylinder(0, WIDTH_CHUNKS);
            assertTrue(DhFold.isNearestSection(cylinder, Direction.Axis.Z, SNAP, LEAF, 0, 5 * WIDTH_BLOCKS));
            assertFalse(DhFold.isNearestSection(cylinder, Direction.Axis.X, SNAP, LEAF, 0, WIDTH_BLOCKS / 64));
        }
    }

    @Nested
    class TheReloadLandsOnTheCopyTheGateKeeps {
        private static final int SECTIONS_PER_WORLD = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);

        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionWhoseCornerIsNearButWhoseCentreIsFarIsSeatedOnTheFarSide() {
            int ref = 470;
            int section = SECTIONS_PER_WORLD - 1;
            assertFalse(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, LEAF, ref, section));
            assertTrue(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, LEAF, ref, section - SECTIONS_PER_WORLD));
            assertEquals(section - SECTIONS_PER_WORLD, DhFold.nearestSection(lattice, SNAP, LEAF, ref, 0, section, 0).x());
        }

        @Test
        void everyLapCopyOfASectionResolvesToTheOneCopyTheGateKeeps() {
            for (int ref = -600; ref <= 600; ref += 37) {
                for (int section = 0; section < SECTIONS_PER_WORLD; section++) {
                    int kept = DhFold.nearestSection(lattice, SNAP, LEAF, ref, 0, section, 0).x();
                    int drawn = 0;
                    for (int lap = -2; lap <= 2; lap++) {
                        int copy = section + lap * SECTIONS_PER_WORLD;
                        if (DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, LEAF, ref, copy)) {
                            drawn++;
                            assertEquals(kept, copy, "ref " + ref + " section " + section);
                        }
                        assertEquals(kept, DhFold.nearestSection(lattice, SNAP, LEAF, ref, 0, copy, 0).x());
                    }
                    assertEquals(1, drawn, "ref " + ref + " section " + section);
                }
            }
        }
    }

    @Nested
    class TheTreeHoldsOneLapAroundItsCentre {
        private static final int LEAF_WIDTH = DhFold.sectionWidthBlocks(LEAF);
        private static final int REF = 100;
        private static final int HALF = WIDTH_BLOCKS / 2;

        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionInsideTheLapIsHeld() {
            assertTrue(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, 512, 4 * LEAF_WIDTH));
        }

        @Test
        void aSectionPastHalfAWorldIsRefusedAndItsNearCopyIsHeld() {
            assertFalse(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, 640, LEAF_WIDTH));
            assertTrue(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, 640 - WIDTH_BLOCKS, LEAF_WIDTH));
        }

        @Test
        void aSectionTouchingTheLapEdgeFromOutsideIsRefused() {
            assertFalse(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, REF + HALF, LEAF_WIDTH));
            assertFalse(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, REF - HALF - LEAF_WIDTH, LEAF_WIDTH));
            assertTrue(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, REF + HALF - 1, LEAF_WIDTH));
        }

        @Test
        void aSectionWiderThanTheWorldIsHeldOnlyWhereItCoversTheLap() {
            assertTrue(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, 0, 4 * WIDTH_BLOCKS));
            assertFalse(DhFold.overlapsNearestLap(lattice, Direction.Axis.X, REF, 4 * WIDTH_BLOCKS, 4 * WIDTH_BLOCKS));
        }

        @Test
        void theLoopingAxisOfACylinderRefusesPastHalfAWorld() {
            DhLattice cylinder = cylinder(0, WIDTH_CHUNKS);
            assertFalse(DhFold.overlapsNearestLap(cylinder, Direction.Axis.X, REF, REF + HALF, LEAF_WIDTH));
        }

        @Test
        void theUnboundedAxisOfACylinderHoldsAnySection() {
            DhLattice cylinder = cylinder(0, WIDTH_CHUNKS);
            assertTrue(DhFold.overlapsNearestLap(cylinder, Direction.Axis.Z, REF, 9999 * LEAF_WIDTH, LEAF_WIDTH));
            assertTrue(DhFold.overlapsNearestLap(cylinder, Direction.Axis.Z, REF, -9999 * LEAF_WIDTH, LEAF_WIDTH));
        }
    }

    @Nested
    class TheLapIsDecidedAtTheSnapLevelAndInherited {
        private static final byte CHUNK_32 = 9;

        private final DhLattice lattice = torus(-32, 32);

        @Test
        void theSnapCellIsASixteenthOfTheNarrowestLoopAndNeverBelowTheLeaf() {
            assertEquals(6, DhFold.snapDetailLevel(torus(-32, 32), LEAF));
            assertEquals(8, DhFold.snapDetailLevel(torus(-160, 160), LEAF));
            assertEquals(6, DhFold.snapDetailLevel(torus(-16, 16), LEAF));
            assertEquals(6, DhFold.snapDetailLevel(cylinder(0, WIDTH_CHUNKS), LEAF));
            assertEquals(7, DhFold.snapDetailLevel(torus(0, 2 * WIDTH_CHUNKS), LEAF));
        }

        @Test
        void aParentInTheWindowHasEveryChildInIt() {
            for (int ref = -600; ref <= 600; ref += 37) {
                for (byte level = LEAF + 1; level <= WORLD; level++) {
                    for (int section = -6; section <= 6; section++) {
                        if (!DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, level, ref, section)) {
                            continue;
                        }

                        byte child = (byte) (level - 1);
                        String where = "ref " + ref + " level " + level + " section " + section;
                        assertTrue(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, child, ref, 2 * section),
                                where + " first child");
                        assertTrue(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, child, ref, 2 * section + 1),
                                where + " last child");
                    }
                }
            }
        }

        @Test
        void aParentOutsideTheWindowHasNoChildInIt() {
            for (int ref = -600; ref <= 600; ref += 37) {
                for (byte level = LEAF + 1; level <= WORLD; level++) {
                    for (int section = -6; section <= 6; section++) {
                        if (DhFold.overlapsNearestWindow(lattice, Direction.Axis.X, SNAP, level, ref, section)) {
                            continue;
                        }

                        byte child = (byte) (level - 1);
                        String where = "ref " + ref + " level " + level + " section " + section;
                        assertFalse(DhFold.overlapsNearestWindow(lattice, Direction.Axis.X, SNAP, child, ref, 2 * section),
                                where + " first child");
                        assertFalse(DhFold.overlapsNearestWindow(lattice, Direction.Axis.X, SNAP, child, ref, 2 * section + 1),
                                where + " last child");
                    }
                }
            }
        }

        @Test
        void aHalfWorldSectionStraddlesTheWindowEdgeOnOneSideAndLiesWholeOnTheOther() {
            assertTrue(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, CHUNK_32, 200, 0));
            assertFalse(DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, CHUNK_32, 200, -1));
            assertTrue(DhFold.overlapsNearestWindow(lattice, Direction.Axis.X, SNAP, CHUNK_32, 200, -1));
            assertTrue(DhFold.overlapsNearestWindow(lattice, Direction.Axis.X, SNAP, CHUNK_32, 200, 1));
            assertFalse(DhFold.overlapsNearestWindow(lattice, Direction.Axis.X, SNAP, CHUNK_32, 200, 2));
        }

        @Test
        void theWindowAtTheSnapLevelIsOneWorldWide() {
            int cellsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(SNAP);
            for (int ref = -600; ref <= 600; ref += 37) {
                int inside = 0;
                for (int cell = -3 * cellsPerWorld; cell < 3 * cellsPerWorld; cell++) {
                    if (DhFold.isNearestSection(lattice, Direction.Axis.X, SNAP, SNAP, ref, cell)) {
                        inside++;
                    }
                }

                assertEquals(cellsPerWorld, inside, "ref " + ref);
            }
        }
    }

    @Nested
    class TheSeamDistanceIsTheShorterWayRound {
        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);

        @Test
        void twoPointsEitherSideOfTheSeamAreCloseTogether() {
            assertEquals(20, DhFold.seamChebyshevDistance(lattice, 10, 0, WIDTH_BLOCKS - 10, 0));
            assertEquals(20, DhFold.seamChebyshevDistance(lattice, 0, WIDTH_BLOCKS - 10, 0, 10));
        }

        @Test
        void twoPointsOnOneSideKeepTheirPlainDistance() {
            assertEquals(200, DhFold.seamChebyshevDistance(lattice, 100, 0, 300, 0));
        }

        @Test
        void aPointAnyNumberOfLapsOutMeasuresFromItsNearestCopy() {
            assertEquals(0, DhFold.seamChebyshevDistance(lattice, 10, 0, 10 + 3 * WIDTH_BLOCKS, 0));
            assertEquals(24, DhFold.seamChebyshevDistance(lattice, 0, 0, 1000 + WIDTH_BLOCKS, 0));
            assertEquals(24, DhFold.seamChebyshevDistance(lattice, 0, 0, 1000 - 2 * WIDTH_BLOCKS, 0));
        }

        @Test
        void theAntipodeIsHalfAWorldAwayEitherWay() {
            assertEquals(WIDTH_BLOCKS / 2, DhFold.seamChebyshevDistance(lattice, 0, 0, WIDTH_BLOCKS / 2, 0));
            assertEquals(WIDTH_BLOCKS / 2, DhFold.seamChebyshevDistance(lattice, WIDTH_BLOCKS / 2, 0, 0, 0));
        }

        @Test
        void theUnboundedAxisOfACylinderKeepsThePlainDistance() {
            DhLattice cylinder = cylinder(0, WIDTH_CHUNKS);
            assertEquals(5 * WIDTH_BLOCKS, DhFold.seamChebyshevDistance(cylinder, 0, 0, 0, 5 * WIDTH_BLOCKS));
            assertEquals(20, DhFold.seamChebyshevDistance(cylinder, 10, 0, WIDTH_BLOCKS - 10, 0));
        }

        @Test
        void theChebyshevDistanceTakesTheLongerAxisThroughTheSeam() {
            assertEquals(30, DhFold.seamChebyshevDistance(lattice, 10, 5, WIDTH_BLOCKS - 10, WIDTH_BLOCKS - 25));
            assertEquals(300, DhFold.seamChebyshevDistance(lattice, 10, 100, WIDTH_BLOCKS - 10, 400));
        }
    }

    @Nested
    class TheRepositoryKeyIsFoldedByItsType {
        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);
        private final int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(DhKeys.LEAF);

        @Test
        void aSectionKeyPastTheSeamFoldsIntoTheWorld() {
            long folded = (Long) DhKeys.foldKey(lattice, DhSectionPos.encode(DhKeys.LEAF, sectionsPerWorld, 1));
            assertEquals(DhKeys.LEAF, DhSectionPos.getDetailLevel(folded));
            assertEquals(0, DhSectionPos.getX(folded));
            assertEquals(1, DhSectionPos.getZ(folded));
        }

        @Test
        void aChunkKeyPastTheSeamFoldsIntoTheWorld() {
            DhChunkPos folded = (DhChunkPos) DhKeys.foldKey(lattice, new DhChunkPos(WIDTH_CHUNKS, 1));
            assertEquals(0, folded.getX());
            assertEquals(1, folded.getZ());
        }

        @Test
        void aBlockKeyPastTheSeamFoldsIntoTheWorld() {
            DhBlockPos folded = (DhBlockPos) DhKeys.foldKey(lattice, new DhBlockPos(WIDTH_BLOCKS, 64, 1));
            assertEquals(0, folded.getX());
            assertEquals(64, folded.getY());
            assertEquals(1, folded.getZ());
        }

        @Test
        void aKeyInsideTheWorldComesBackUntouched() {
            DhChunkPos inside = new DhChunkPos(3, 4);
            assertSame(inside, DhKeys.foldKey(lattice, inside));
        }

        @Test
        void anUnwrappedLevelFoldsNothing() {
            DhChunkPos past = new DhChunkPos(WIDTH_CHUNKS, 1);
            assertSame(past, DhKeys.foldKey(null, past));
        }

        @Test
        void aKeyOfAnUnknownTypeComesBackUntouched() {
            String key = "not a position";
            assertSame(key, DhKeys.foldKey(lattice, key));
        }
    }

    @Nested
    class TheStatementSeesTheFoldedKeyAndTheDtoKeepsItsOwn {
        private final DhLattice lattice = torus(0, WIDTH_CHUNKS);

        @Test
        void aChunkHashRowIsWrittenUnderTheFoldedKey() {
            ChunkHashDTO dto = new ChunkHashDTO(new DhChunkPos(WIDTH_CHUNKS, 1), 7);
            DhChunkPos seen = DhKeys.withFoldedKey(lattice, dto, () -> dto.pos);
            assertEquals(0, seen.getX());
            assertEquals(1, seen.getZ());
            assertEquals(WIDTH_CHUNKS, dto.pos.getX());
        }

        @Test
        void aBeaconBeamRowIsWrittenUnderTheFoldedKey() {
            BeaconBeamDTO dto = new BeaconBeamDTO(new DhBlockPos(WIDTH_BLOCKS, 64, 1), Color.WHITE);
            DhBlockPos seen = DhKeys.withFoldedKey(lattice, dto, () -> dto.blockPos);
            assertEquals(0, seen.getX());
            assertEquals(1, seen.getZ());
            assertEquals(WIDTH_BLOCKS, dto.blockPos.getX());
        }

        @Test
        void aThrownStatementStillGivesTheDtoItsKeyBack() {
            ChunkHashDTO dto = new ChunkHashDTO(new DhChunkPos(WIDTH_CHUNKS, 1), 7);
            assertThrows(IllegalStateException.class, () -> DhKeys.withFoldedKey(lattice, dto, () -> {
                throw new IllegalStateException();
            }));
            assertEquals(WIDTH_CHUNKS, dto.pos.getX());
        }

        @Test
        void anUnwrappedLevelLeavesTheKeyAloneThroughout() {
            DhChunkPos raw = new DhChunkPos(WIDTH_CHUNKS, 1);
            ChunkHashDTO dto = new ChunkHashDTO(raw, 7);
            assertSame(raw, DhKeys.withFoldedKey(null, dto, () -> dto.pos));
            assertSame(raw, dto.pos);
        }
    }
}
