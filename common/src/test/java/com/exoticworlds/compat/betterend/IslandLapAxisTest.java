package com.exoticworlds.compat.betterend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WrapDomain;

class IslandLapAxisTest {
    private static final int[] LAPS = {512, 2048, 4096, 16384};

    private static final double[] DISTANCES = {1.0, 60.0, 150.0, 300.0, 4096.0, 8192.0};

    private static final double EPSILON = 1.0E-9;

    @Test
    void aLoopedAxisKeepsBetterEndsDistanceOrShrinksItOnlyToFitAWholeCountPerLap() {
        for (int lap : LAPS) {
            for (double distance : DISTANCES) {
                IslandLapAxis axis = IslandLapAxis.of(new WrapDomain(-lap / 2, lap / 2), distance);
                String name = "distance " + distance + " on a lap of " + lap + ": " + axis;
                assertTrue(axis.cellBlocks() <= distance + EPSILON, name + " widens the grid");
                assertEquals(lap, axis.cells() * axis.cellBlocks(), EPSILON, name + " does not close on the lap");
                assertTrue(axis.cells() == 1 || (axis.cells() - 1) * distance < lap,
                        name + " shrinks the grid further than a whole count per lap needs");
            }
        }
    }

    @Test
    void aLapThatHoldsTheDistanceWholeKeepsIt() {
        IslandLapAxis axis = IslandLapAxis.of(new WrapDomain(-1024, 1024), 256.0);
        assertEquals(8, axis.cells());
        assertEquals(256.0, axis.cellBlocks(), EPSILON);
    }

    @Test
    void anUnboundedAxisKeepsBetterEndsGridAsItIs() {
        IslandLapAxis axis = IslandLapAxis.of(new WrapDomain.Noop(), 300.0);
        assertFalse(axis.loops());
        assertEquals(300.0, axis.cellBlocks(), EPSILON);
        assertEquals(-7, axis.wrap(-7));
        assertEquals(0, axis.shift(-7));
        assertEquals(-7, axis.nearestToOrigin(-7));
        assertEquals(0, axis.originCopy(12345.0));
    }

    @Test
    void aCellOneLapAwayIsTheSameCellOneLapShifted() {
        IslandLapAxis axis = IslandLapAxis.of(new WrapDomain(-2048, 2048), 300.0);
        for (int cell = -3 * axis.cells(); cell < 3 * axis.cells(); cell++) {
            assertEquals(axis.wrap(cell), axis.wrap(cell + axis.cells()), "cell " + cell);
            assertEquals(axis.shift(cell) + axis.lapBlocks(), axis.shift(cell + axis.cells()), "cell " + cell);
            assertEquals(cell, axis.wrap(cell) + axis.shift(cell) / axis.lapBlocks() * axis.cells(), "cell " + cell);
        }
    }

    @Test
    void theCellNearestTheOriginIsReadTheShorterWayRound() {
        IslandLapAxis axis = IslandLapAxis.of(new WrapDomain(-2048, 2048), 300.0);
        assertEquals(0, axis.nearestToOrigin(0));
        assertEquals(-1, axis.nearestToOrigin(axis.cells() - 1));
        for (int cell = 0; cell < axis.cells(); cell++) {
            int nearest = axis.nearestToOrigin(cell);
            assertTrue(Math.abs(nearest + 0.5) <= Math.abs(nearest + 0.5 + (nearest < 0 ? 1 : -1) * axis.cells()),
                    "cell " + cell + " read as " + nearest);
        }
    }

    @Test
    void theOriginCopyIsTheNearestWholeLap() {
        IslandLapAxis axis = IslandLapAxis.of(new WrapDomain(-2048, 2048), 300.0);
        assertEquals(0, axis.originCopy(2047.0));
        assertEquals(4096, axis.originCopy(2049.0));
        assertEquals(-4096, axis.originCopy(-2049.0));
    }
}
