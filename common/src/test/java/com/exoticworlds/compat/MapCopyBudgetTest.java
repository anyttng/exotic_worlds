package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

class MapCopyBudgetTest {
    private static final int BLIT_BUDGET = 16384;

    @Test
    void theCapSpendsTheBlitBudgetOverTheTilesWithContent() {
        assertEquals(63, MapCopyBudget.copyRangeCap(2, 1), "one tile on a torus: (sqrt(16384) - 1) / 2 = 63");
        assertEquals(8191, MapCopyBudget.copyRangeCap(1, 1), "one tile on a cylinder: (16384 - 1) / 2 = 8191");
        assertEquals(15, MapCopyBudget.copyRangeCap(2, 16), "16 tiles on a torus: (sqrt(1024) - 1) / 2 = 15");
        assertEquals(511, MapCopyBudget.copyRangeCap(1, 16), "16 tiles on a cylinder: (1024 - 1) / 2 = 511");
        assertEquals(0, MapCopyBudget.copyRangeCap(2, BLIT_BUDGET / 4), "4 blits per tile leave no torus copy");
        assertEquals(1, MapCopyBudget.copyRangeCap(1, BLIT_BUDGET / 4), "4 blits per tile leave one cylinder copy per side");
        assertEquals(0, MapCopyBudget.copyRangeCap(0, 1), "no looped axis got copies");
    }

    @Test
    void theRangeReachesTheFarthestLapTheViewTouches() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {2, 2},
                MapCopyBudget.copyRanges(tiny, tiny, 1, new int[] {-1000, 1000}, new int[] {-1000, 1000}, MapCopies.REPEATED),
                "-1000..999 touches laps -2..2 of a 512-block world");
    }

    @Test
    void theCapBindsOnlyWhenTheGridsTilesTimesTheLapsOverrunTheBudget() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        int[] far = {-50_000, 50_000};
        assertArrayEquals(new int[] {15, 15}, MapCopyBudget.copyRanges(tiny, tiny, 16, far, far, MapCopies.REPEATED),
                "16 tiles over 197 x 197 laps overrun 16384 blits, so the torus cap (sqrt(1024) - 1) / 2 = 15 binds");
        assertArrayEquals(new int[] {98, 0},
                MapCopyBudget.copyRanges(tiny, AxisCopies.UNBOUNDED, 16, far, far, MapCopies.REPEATED),
                "16 tiles over 197 laps of a cylinder stay inside the budget");
    }

    @Test
    void anUnboundedAxisDrawsNoCopies() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {0, 2}, MapCopyBudget.copyRanges(AxisCopies.UNBOUNDED, tiny, 1,
                new int[] {-1000, 1000}, new int[] {-1000, 1000}, MapCopies.REPEATED), "an unbounded axis got copies");
    }

    @Test
    void aSingleCopyMapDrawsNoCopyWhateverTheViewAsks() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {0, 0},
                MapCopyBudget.copyRanges(tiny, tiny, 1, new int[] {-1000, 1000}, new int[] {-1000, 1000}, MapCopies.SINGLE),
                "a torus under SINGLE got copies");
    }

    @Test
    void aFewTilesOnAWideWorldStillGetTheCopyTheViewReaches() {
        AxisCopies axis = AxisCopies.looped(-14992, 30000);
        assertArrayEquals(new int[] {1, 0},
                MapCopyBudget.copyRanges(axis, axis, 8, new int[] {14024, 15944}, new int[] {-960, 960}, MapCopies.REPEATED),
                "8 tiles on a 60 x 60-region world, a view crossing the +X seam and none along Z");
    }

    @Test
    void thePaintedBoxSpansEveryCopyDrawn() {
        Copies copies = MapCopyBudget.painted(AxisCopies.looped(0, 512), 1, AxisCopies.looped(0, 512), 2);
        assertEquals(2, copies.reach(), "the reach is the wider of the two ranges");
        assertEquals(-512, copies.painted().minX(), "one copy left of a world starting at 0");
        assertEquals(1023, copies.painted().maxX(), "one copy right of a 512-block world, last block inclusive");
        assertEquals(-1024, copies.painted().minZ(), "two copies below");
        assertEquals(1535, copies.painted().maxZ(), "two copies above");
    }

    @Test
    void anUnboundedAxisPaintsEverywhere() {
        Copies copies = MapCopyBudget.painted(AxisCopies.UNBOUNDED, 0, AxisCopies.looped(0, 512), 0);
        assertEquals(Integer.MIN_VALUE, copies.painted().minX(), "an unbounded axis was bounded");
        assertEquals(Integer.MAX_VALUE, copies.painted().maxX(), "an unbounded axis was bounded");
        assertEquals(0, copies.painted().minZ(), "the looped axis kept its own bounds");
        assertEquals(511, copies.painted().maxZ(), "the looped axis kept its own bounds");
    }
}
