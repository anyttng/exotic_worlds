package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AxisCopiesTest {
    private static final int MIN = -512;
    private static final int WIDTH = 1024;

    @Test
    void aLoopedAxisNamesItsBounds() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertTrue(axis.loops(), "the looped axis reads as unbounded");
        assertEquals(MIN, axis.min(), "the first block moved");
        assertEquals(MIN + WIDTH, axis.max(), "the exclusive bound is not min + width");
        assertEquals(WIDTH, axis.width(), "the width is not the extent");
    }

    @Test
    void clampViewKeepsTheViewInsideTheCanonicalCopy() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertEquals(100.5, axis.clampView(100.5, 200.0), "a view inside the world moved");
        assertEquals(MIN + 200.0, axis.clampView(MIN - 3000.0, 200.0), "a view past the first edge is not stopped 200 inside it");
        assertEquals(MIN + WIDTH - 200.0, axis.clampView(MIN + WIDTH + 3000.0, 200.0),
                "a view past the last edge is not stopped 200 inside it");
        assertEquals(0.0, axis.clampView(300.0, 600.0), "a view wider than the world is not centred on it at 0");
        assertEquals(-5000.0, AxisCopies.UNBOUNDED.clampView(-5000.0, 200.0), "an unbounded axis clamped a view");
    }

    @Test
    void anUnboundedAxisHasNoExtent() {
        AxisCopies axis = AxisCopies.UNBOUNDED;
        assertFalse(axis.loops(), "the unbounded axis reads as looped");
        assertEquals(0, axis.width(), "an unbounded axis has a width");
    }

    @Test
    void aLoopedAxisClipsASpanToTheWorld() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertEquals(MIN, axis.clipMin(MIN - 100), "a span starting before the world was not clipped to min");
        assertEquals(MIN + 100, axis.clipMin(MIN + 100), "a span starting inside the world was moved");
        assertEquals(MIN + WIDTH, axis.clipMax(MIN + WIDTH + 100), "a span ending past the world was not clipped to max");
        assertEquals(MIN + WIDTH - 100, axis.clipMax(MIN + WIDTH - 100), "a span ending inside the world was moved");
    }

    @Test
    void anUnboundedAxisClipsNothing() {
        AxisCopies axis = AxisCopies.UNBOUNDED;
        assertEquals(-40000000, axis.clipMin(-40000000), "an unbounded axis clipped a span start");
        assertEquals(40000000, axis.clipMax(40000000), "an unbounded axis clipped a span end");
    }

    @Test
    void anUnboundedAxisHasNoBounds() {
        AxisCopies axis = AxisCopies.UNBOUNDED;
        assertThrows(IllegalStateException.class, axis::min, "min answered on an unbounded axis");
        assertThrows(IllegalStateException.class, axis::max, "max answered on an unbounded axis");
    }

    @Test
    void aLoopedAxisNeedsAWidth() {
        assertThrows(IllegalArgumentException.class, () -> AxisCopies.looped(MIN, 0), "a zero width was accepted");
    }

    @Test
    void withinOneLapStopsTheCoordinateAWorldShortOfLappingTheAnchor() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertEquals(100, axis.withinOneLap(0, 100), "a coordinate inside one lap moved");
        assertEquals(WIDTH - 1, axis.withinOneLap(0, WIDTH - 1), "the last coordinate of the lap moved");
        assertEquals(WIDTH - 1, axis.withinOneLap(0, 3000), "a coordinate past the lap is not stopped at width - 1");
        assertEquals(-(WIDTH - 1), axis.withinOneLap(0, -3000), "a coordinate a lap back is not stopped at -(width - 1)");
        assertEquals(5000, AxisCopies.UNBOUNDED.withinOneLap(0, 5000), "an unbounded axis has laps");
    }
}
