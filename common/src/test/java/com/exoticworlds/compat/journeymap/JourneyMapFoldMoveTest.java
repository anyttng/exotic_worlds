package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;

class JourneyMapFoldMoveTest {
    private static final double CENTER = 0.1;
    private static final double DELTA = 0.2;
    private static final double HALF_SPAN = 10.0;

    @Test
    void anUnboundedAxisPassesTheDeltaItself() {
        assertEquals(DELTA, JourneyMapFold.clampedMove(AxisCopies.UNBOUNDED, CENTER, DELTA, HALF_SPAN), 0.0,
                "0.1 + 0.2 - 0.1 rebuilt is 0.20000000000000004");
    }

    @Test
    void aLoopedAxisAwayFromTheEdgePassesTheDeltaItself() {
        assertEquals(DELTA, JourneyMapFold.clampedMove(AxisCopies.looped(-256, 512), CENTER, DELTA, HALF_SPAN), 0.0);
    }

    @Test
    void aLoopedAxisStopsTheMoveAtTheEdge() {
        assertEquals(6.0, JourneyMapFold.clampedMove(AxisCopies.looped(-256, 512), 240.0, 20.0, HALF_SPAN), 0.0,
                "the view stops where its half span meets the upper bound 256");
    }
}
