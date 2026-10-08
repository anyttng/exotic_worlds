package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;

class XaeroWorldMapFoldLapsTest {
    private static final AxisCopies AXIS = AxisCopies.looped(-512, 1024);

    @Test
    void aRepeatedMapDrawsEveryLapTheViewTouches() {
        assertArrayEquals(new int[] {-1, 0, 1}, XaeroWorldMapFold.drawnLaps(AXIS, -1536, 1536, MapCopies.REPEATED),
                "three worlds in view are not three laps");
        assertArrayEquals(new int[] {3}, XaeroWorldMapFold.drawnLaps(AXIS, 3000, 3100, MapCopies.REPEATED),
                "a view in lap 3 does not draw lap 3");
    }

    @Test
    void aSingleCopyMapDrawsTheCanonicalLapAlone() {
        assertArrayEquals(new int[] {0}, XaeroWorldMapFold.drawnLaps(AXIS, -1536, 1536, MapCopies.SINGLE),
                "the laps beside the world were drawn under SINGLE");
        assertArrayEquals(new int[] {0}, XaeroWorldMapFold.drawnLaps(AXIS, 500, 600, MapCopies.SINGLE),
                "a view across the seam at 512 lost the canonical lap or kept lap 1");
    }

    @Test
    void aSingleCopyMapDrawsNothingForAViewPastTheWorld() {
        assertArrayEquals(new int[0], XaeroWorldMapFold.drawnLaps(AXIS, 3000, 3100, MapCopies.SINGLE),
                "a view in lap 3 drew a copy under SINGLE");
    }

    @Test
    void anUnboundedAxisDrawsItsOneLapInBothModes() {
        assertArrayEquals(new int[] {0},
                XaeroWorldMapFold.drawnLaps(AxisCopies.UNBOUNDED, -40000000, 40000000, MapCopies.SINGLE),
                "an unbounded axis lost its one lap under SINGLE");
    }
}
