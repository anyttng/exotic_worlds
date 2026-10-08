package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;

class JourneyMapFoldCopyRangeTest {
    private static final int BLIT_BUDGET = 16384;

    @Test
    void theCapSpendsTheBlitBudgetOverTheTilesWithContent() {
        assertEquals(63, JourneyMapFold.copyRangeCap(2, 1), "one tile on a torus: (sqrt(16384) - 1) / 2 = 63");
        assertEquals(8191, JourneyMapFold.copyRangeCap(1, 1), "one tile on a cylinder: (16384 - 1) / 2 = 8191");
        assertEquals(15, JourneyMapFold.copyRangeCap(2, 16), "16 tiles on a torus: (sqrt(1024) - 1) / 2 = 15");
        assertEquals(511, JourneyMapFold.copyRangeCap(1, 16), "16 tiles on a cylinder: (1024 - 1) / 2 = 511");
        assertEquals(0, JourneyMapFold.copyRangeCap(2, BLIT_BUDGET / 4), "4 blits per tile leave no torus copy");
        assertEquals(1, JourneyMapFold.copyRangeCap(1, BLIT_BUDGET / 4), "4 blits per tile leave one cylinder copy per side");
        assertEquals(0, JourneyMapFold.copyRangeCap(0, 1), "no looped axis got copies");
    }

    @Test
    void theRangeReachesTheFarthestLapTheViewTouches() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {2, 2},
                JourneyMapFold.copyRanges(tiny, tiny, 1, new int[] {-1000, 1000}, new int[] {-1000, 1000}, MapCopies.REPEATED),
                "-1000..999 touches laps -2..2 of a 512-block world");
    }

    @Test
    void theCapBindsOnlyWhenTheGridsTilesTimesTheLapsOverrunTheBudget() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        int[] far = {-50_000, 50_000};
        assertArrayEquals(new int[] {15, 15}, JourneyMapFold.copyRanges(tiny, tiny, 16, far, far, MapCopies.REPEATED),
                "16 tiles over 197 x 197 laps overrun 16384 blits, so the torus cap (sqrt(1024) - 1) / 2 = 15 binds");
        assertArrayEquals(new int[] {98, 0},
                JourneyMapFold.copyRanges(tiny, AxisCopies.UNBOUNDED, 16, far, far, MapCopies.REPEATED),
                "16 tiles over 197 laps of a cylinder stay inside the budget");
    }

    @Test
    void anUnboundedAxisDrawsNoCopies() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {0, 2}, JourneyMapFold.copyRanges(AxisCopies.UNBOUNDED, tiny, 1,
                new int[] {-1000, 1000}, new int[] {-1000, 1000}, MapCopies.REPEATED), "an unbounded axis got copies");
    }

    @Test
    void aSingleCopyMapDrawsNoCopyWhateverTheViewAsks() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {0, 0},
                JourneyMapFold.copyRanges(tiny, tiny, 1, new int[] {-1000, 1000}, new int[] {-1000, 1000}, MapCopies.SINGLE),
                "a torus under SINGLE got copies");
    }

    @Test
    void aFewTilesOnAWideWorldStillGetTheCopyTheViewReaches() {
        AxisCopies axis = AxisCopies.looped(-14992, 30000);
        assertArrayEquals(new int[] {1, 0},
                JourneyMapFold.copyRanges(axis, axis, 8, new int[] {14024, 15944}, new int[] {-960, 960}, MapCopies.REPEATED),
                "8 tiles on a 60 x 60-region world, a view crossing the +X seam and none along Z");
    }

    @Test
    void theViewSpanIsTheWindowInBlocksAroundTheCenter() {
        assertArrayEquals(new int[] {-640, 640}, JourneyMapFold.viewSpan(0.0, 1280, 512),
                "1280 px at 512 px per 512-block region is 1280 blocks, 640 each side");
        assertArrayEquals(new int[] {100 - 5120, 100 + 5120}, JourneyMapFold.viewSpan(100.0, 1280, 64),
                "1280 px at 64 px per region is 10240 blocks, 5120 each side of the center at 100");
        assertArrayEquals(new int[] {(int) Math.floor(100.5 - 640), (int) Math.ceil(100.5 + 640)},
                JourneyMapFold.viewSpan(100.5, 1280, 512), "a fractional center does not floor and ceil outward");
    }
}
