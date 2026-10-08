package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;

class JourneyMapFoldGridRegionsTest {
    private static final AxisCopies ON_GRID_CHUNKS = AxisCopies.looped(-64, 128);
    private static final AxisCopies OFF_GRID_CHUNKS = AxisCopies.looped(-937, 1875);
    private static final AxisCopies OFF_GRID_BLOCKS = AxisCopies.looped(-14992, 30000);

    @Test
    void anUnboundedAxisKeepsTheGridsOwnRegions() {
        assertArrayEquals(new int[] {-2, -1, 0, 1, 2, 3}, JourneyMapFold.foldedRegions(AxisCopies.UNBOUNDED, -2, 3));
    }

    @Test
    void aGridInsideTheWorldKeepsItsOwnRegions() {
        assertArrayEquals(new int[] {0, 1, 2}, JourneyMapFold.foldedRegions(OFF_GRID_CHUNKS, 0, 2));
    }

    @Test
    void aGridPastTheSeamOnTheRegionGridFoldsOntoTheFarEdge() {
        assertArrayEquals(new int[] {-2, 1}, JourneyMapFold.foldedRegions(ON_GRID_CHUNKS, 1, 2),
                "raw region 2 is chunks 64..95, which fold to -64..-33, region -2");
    }

    @Test
    void aGridPastTheSeamOffTheRegionGridTakesEveryRegionItsChunksFoldInto() {
        assertArrayEquals(new int[] {-30, -29, -28, 29}, JourneyMapFold.foldedRegions(OFF_GRID_CHUNKS, 29, 30),
                "chunks 938..991 lie past the +X edge at 938 and fold to -937..-884, regions -30..-28");
    }

    @Test
    void aGridWhollyPastTheMinEdgeShiftsByOneWidth() {
        assertArrayEquals(new int[] {26, 27, 28}, JourneyMapFold.foldedRegions(OFF_GRID_CHUNKS, -32, -31),
                "chunks -1024..-961 fold to 851..914");
    }

    @Test
    void aGridWiderThanTheWorldTakesEveryRegionOnce() {
        assertArrayEquals(new int[] {-2, -1, 0, 1}, JourneyMapFold.foldedRegions(ON_GRID_CHUNKS, -3, 2));
    }

    @Test
    void anUnboundedAxisShowsOnlyTheRegionsInsideTheBounds() {
        assertTrue(JourneyMapFold.regionInView(AxisCopies.UNBOUNDED, 3, 0.0, 4.0));
        assertFalse(JourneyMapFold.regionInView(AxisCopies.UNBOUNDED, 4, 0.0, 4.0), "the bounds' max is exclusive");
    }

    @Test
    void aTileWhoseCopyMeetsTheBoundsIsShown() {
        assertTrue(JourneyMapFold.regionInView(OFF_GRID_BLOCKS, -30, 25.0, 36.0),
                "region -30 is blocks -15360..-14849, its +1 lap copy 14640..15151 lies inside 12800..18431");
        assertTrue(JourneyMapFold.regionInView(OFF_GRID_BLOCKS, 26, 25.0, 36.0));
    }

    @Test
    void aTileNoCopyOfWhichMeetsTheBoundsIsHidden() {
        assertFalse(JourneyMapFold.regionInView(OFF_GRID_BLOCKS, 0, 25.0, 36.0),
                "region 0's copies sit at 0 and at 30000 blocks, both outside 12800..18431");
    }
}
