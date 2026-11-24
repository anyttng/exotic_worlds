package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.fold.WrapDomain;

class SpreadAreaTest {
    private static final WorldLoopBounds BOUNDS = WorldLoopBounds.ofWidth(64);
    private static final int WIDTH_BLOCKS = 64 * 16;
    private static final int HALF = WIDTH_BLOCKS / 2;
    private static final int SKEW_BLOCKS = 5 * 16;

    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(BOUNDS));
    private static final WorldFold SKEWED = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, 5));

    @Test
    void anAreaCoveringTheWorldWrapsBothAxesWithoutClamping() {
        SpreadArea.Confined confined = SpreadArea.confine(TORUS, HALF + 88.0, -HALF - 188.0,
                -WIDTH_BLOCKS, -WIDTH_BLOCKS, WIDTH_BLOCKS, WIDTH_BLOCKS);

        assertEquals(-HALF + 88.0, confined.x());
        assertEquals(HALF - 188.0, confined.z());
        assertFalse(confined.isClamped());
    }

    @Test
    void anAreaNarrowerThanTheWorldClampsItsClosedAxis() {
        SpreadArea.Confined confined = SpreadArea.confine(TORUS, 300.0, -300.0, -100.0, -WIDTH_BLOCKS, 100.0, WIDTH_BLOCKS);

        assertEquals(100.0, confined.x());
        assertEquals(-300.0, confined.z());
        assertTrue(confined.isClamped());
    }

    @Test
    void aZLapOnTheSkewedLatticeShiftsXByTheSkew() {
        SpreadArea.Confined confined = SpreadArea.confine(SKEWED, 0.0, HALF + 88.0,
                -WIDTH_BLOCKS, -WIDTH_BLOCKS, WIDTH_BLOCKS, WIDTH_BLOCKS);

        assertEquals(-SKEW_BLOCKS, confined.x());
        assertEquals(-HALF + 88.0, confined.z());
        assertFalse(confined.isClamped());
    }

    @Test
    void anAreaFitsInHalfTheWorldOnlyWhileNeitherDiagonalFolds() {
        assertTrue(SpreadArea.canFitInHalfTheWorld(TORUS, HALF - 12.0, HALF - 12.0));
        assertFalse(SpreadArea.canFitInHalfTheWorld(TORUS, HALF + 88.0, 0.0));
        assertFalse(SpreadArea.canFitInHalfTheWorld(TORUS, 0.0, HALF + 88.0));
    }

    @Test
    void aRandomRangeOpensToTheLapOnlyWhereTheAreaCoversALoopedAxis() {
        WrapDomain looped = TORUS.getBlockLattice().getX();
        WrapDomain unbounded = new WrapDomain.Noop();

        assertEquals(-HALF, SpreadArea.randomMin(looped, -WIDTH_BLOCKS, WIDTH_BLOCKS));
        assertEquals(HALF, SpreadArea.randomMax(looped, -WIDTH_BLOCKS, WIDTH_BLOCKS));
        assertEquals(-100.0, SpreadArea.randomMin(looped, -100.0, 100.0));
        assertEquals(100.0, SpreadArea.randomMax(looped, -100.0, 100.0));
        assertEquals(-WIDTH_BLOCKS, SpreadArea.randomMin(unbounded, -WIDTH_BLOCKS, WIDTH_BLOCKS));
        assertEquals(WIDTH_BLOCKS, SpreadArea.randomMax(unbounded, -WIDTH_BLOCKS, WIDTH_BLOCKS));
    }
}
