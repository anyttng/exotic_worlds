package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.world.phys.Vec3;

class SeamBorderTest {
    private static final WorldLoopBounds BOUNDS = WorldLoopBounds.ofWidth(64);
    private static final int WIDTH = 64 * 16;
    private static final int SKEW = 5 * 16;
    private static final double HALF = 100.0;

    private static final SeamBorder TORUS = border(FlatShape.torus(BOUNDS));
    private static final SeamBorder SKEWED = border(FlatShape.latticeTorus(BOUNDS, 5));

    private static SeamBorder border(FlatShape shape) {
        return new SeamBorder(WorldFolds.of(shape).blockLattice(), -HALF, HALF, -HALF, HALF);
    }

    @Test
    void onTheTorusTheBorderAcrossTheSeamIsTheNearestOne() {
        assertTrue(TORUS.inside(WIDTH - 74.0, 0.0, 0.0));
        assertEquals(26.0, TORUS.distanceToEdge(WIDTH - 74.0, 0.0));
        assertEquals(new Vec3(HALF - 1.0E-5F, 64.0, 0.0), TORUS.clamped(300.0, 64.0, 0.0));
    }

    @Test
    void onASkewedWorldACopyAcrossTheZSeamIsInsideOnlyThroughItsXShift() {
        double x = 90.0 + SKEW;
        double z = -4.0 + WIDTH;

        assertTrue(SKEWED.inside(x, z, 0.0));
        assertFalse(TORUS.inside(x, z, 0.0));
        assertEquals(10.0, SKEWED.distanceToEdge(x, z));
        assertEquals(new Vec3(x, 64.0, z), SKEWED.clamped(x, 64.0, z));
    }

    @Test
    void onASkewedWorldAPointOutsideClampsIntoTheBoxOfItsNearestCopy() {
        Vec3 clamped = SKEWED.clamped(300.0 + SKEW, 64.0, WIDTH);

        assertEquals(SKEW + HALF - 1.0E-5F, clamped.x);
        assertEquals(WIDTH, clamped.z);
    }

    @Test
    void theSkewedWallStandsOnTheLatticeCopies() {
        assertTrue(SKEWED.wallShifts().contains(new Vec3(SKEW, 0.0, WIDTH)));
        assertTrue(SKEWED.wallShifts().contains(new Vec3(-SKEW, 0.0, -WIDTH)));
        assertTrue(SKEWED.wallShifts().contains(new Vec3(WIDTH, 0.0, 0.0)));
        assertEquals(9, SKEWED.wallShifts().size());
    }
}
