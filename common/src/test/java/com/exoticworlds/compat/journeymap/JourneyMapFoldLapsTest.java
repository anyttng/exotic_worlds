package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;

class JourneyMapFoldLapsTest {
    private static final int TILE = 512;
    private static final AxisCopies WIDE = AxisCopies.looped(-14992, 30000);

    @Test
    void aTileAcrossTheEdgeIsDrawnInTheLapTheViewReaches() {
        assertArrayEquals(new int[] {1}, JourneyMapFold.tileLaps(WIDE, -15360, TILE, 14024, 15944, 1),
                "region -30 at lap 1 covers 14640..15151, inside the view; at lap 0 it is 30000 blocks away");
    }

    @Test
    void aTileInsideTheViewIsDrawnOnlyWhereItIs() {
        assertArrayEquals(new int[] {0}, JourneyMapFold.tileLaps(WIDE, 14848, TILE, 14024, 15944, 1));
    }

    @Test
    void theRangeCutsTheLapsTheViewWouldTake() {
        assertArrayEquals(new int[0], JourneyMapFold.tileLaps(WIDE, -15360, TILE, 14024, 15944, 0),
                "a range of 0 leaves region -30 no copy");
    }

    @Test
    void aViewWiderThanTheWorldTakesEveryLapInRange() {
        AxisCopies tiny = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {-2, -1, 0, 1}, JourneyMapFold.tileLaps(tiny, 0, TILE, -1000, 1000, 2),
                "region 0 meets -1000..999 at laps -2..1; at lap 2 it starts at 1024, past the view");
    }

    @Test
    void anUnboundedAxisKeepsTheTileWhereItIs() {
        assertArrayEquals(new int[] {0}, JourneyMapFold.tileLaps(AxisCopies.UNBOUNDED, 14848, TILE, 0, 100, 3));
    }
}
