package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

class SpreadAreaTest {
    private static final WorldLoopBounds BOUNDS = WorldLoopBounds.ofWidth(64);
    private static final int WIDTH = 64 * 16;
    private static final int HALF = WIDTH / 2;
    private static final int SKEW = 5 * 16;

    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(BOUNDS));
    private static final WorldFold SKEWED = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, 5));

    @Test
    void anAreaCoveringTheWorldWrapsBothAxesWithoutClamping() {
        SpreadArea.Confined confined = SpreadArea.confine(TORUS, HALF + 88.0, -HALF - 188.0,
                -WIDTH, -WIDTH, WIDTH, WIDTH);

        assertEquals(-HALF + 88.0, confined.x());
        assertEquals(HALF - 188.0, confined.z());
        assertFalse(confined.clamped());
    }

    @Test
    void anAreaNarrowerThanTheWorldClampsItsClosedAxis() {
        SpreadArea.Confined confined = SpreadArea.confine(TORUS, 300.0, -300.0, -100.0, -WIDTH, 100.0, WIDTH);

        assertEquals(100.0, confined.x());
        assertEquals(-300.0, confined.z());
        assertTrue(confined.clamped());
    }

    @Test
    void aZLapOnTheSkewedLatticeShiftsXByTheSkew() {
        SpreadArea.Confined confined = SpreadArea.confine(SKEWED, 0.0, HALF + 88.0,
                -WIDTH, -WIDTH, WIDTH, WIDTH);

        assertEquals(-SKEW, confined.x());
        assertEquals(-HALF + 88.0, confined.z());
        assertFalse(confined.clamped());
    }

    @Test
    void anAreaFitsInHalfTheWorldOnlyWhileNeitherDiagonalFolds() {
        assertTrue(SpreadArea.fitsInHalfTheWorld(TORUS, HALF - 12.0, HALF - 12.0));
        assertFalse(SpreadArea.fitsInHalfTheWorld(TORUS, HALF + 88.0, 0.0));
        assertFalse(SpreadArea.fitsInHalfTheWorld(TORUS, 0.0, HALF + 88.0));
    }

    @Test
    void aRandomRangeOpensToTheLapOnlyWhereTheAreaCoversALoopedAxis() {
        AxisBounds looped = BOUNDS.x();

        assertEquals(-HALF, SpreadArea.randomMin(looped, -WIDTH, WIDTH));
        assertEquals(HALF, SpreadArea.randomMax(looped, -WIDTH, WIDTH));
        assertEquals(-100.0, SpreadArea.randomMin(looped, -100.0, 100.0));
        assertEquals(100.0, SpreadArea.randomMax(looped, -100.0, 100.0));
        assertEquals(-WIDTH, SpreadArea.randomMin(AxisBounds.Unbounded.INSTANCE, -WIDTH, WIDTH));
        assertEquals(WIDTH, SpreadArea.randomMax(AxisBounds.Unbounded.INSTANCE, -WIDTH, WIDTH));
    }
}
