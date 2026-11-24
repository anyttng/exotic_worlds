package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.world.phys.Vec3;

class SeamBorderTest {
    private static final WorldLoopBounds BOUNDS = WorldLoopBounds.ofWidth(64);
    private static final int WIDTH_BLOCKS = 64 * 16;
    private static final int SKEW_BLOCKS = 5 * 16;
    private static final double HALF = 100.0;
    private static final int LONG_SKEW_CHUNKS = 300;
    private static final double LONG_CENTER = 1000.0;
    private static final long LONG_SEED = 20261009L;
    private static final int LONG_SAMPLES = 4000;

    private static final SeamBorder TORUS = border(ShapeFixtures.torus(BOUNDS));
    private static final SeamBorder SKEWED = border(FlatShape.latticeTorus(BOUNDS, 5));

    private static SeamBorder border(FlatShape shape) {
        return new SeamBorder(WorldFolds.of(shape).getBlockLattice(), -HALF, HALF, -HALF, HALF);
    }

    @Test
    void onTheTorusTheBorderAcrossTheSeamIsTheNearestOne() {
        assertTrue(TORUS.isInside(WIDTH_BLOCKS - 74.0, 0.0, 0.0));
        assertEquals(26.0, TORUS.distanceToEdgeBlocks(WIDTH_BLOCKS - 74.0, 0.0));
        assertEquals(new Vec3(HALF - 1.0E-5F, 64.0, 0.0), TORUS.clamped(300.0, 64.0, 0.0));
    }

    @Test
    void onASkewedWorldACopyAcrossTheZSeamIsInsideOnlyThroughItsXShift() {
        double x = 90.0 + SKEW_BLOCKS;
        double z = -4.0 + WIDTH_BLOCKS;

        assertTrue(SKEWED.isInside(x, z, 0.0));
        assertFalse(TORUS.isInside(x, z, 0.0));
        assertEquals(10.0, SKEWED.distanceToEdgeBlocks(x, z));
        assertEquals(new Vec3(x, 64.0, z), SKEWED.clamped(x, 64.0, z));
    }

    @Test
    void onASkewedWorldAPointOutsideClampsIntoTheBoxOfItsNearestCopy() {
        Vec3 clamped = SKEWED.clamped(300.0 + SKEW_BLOCKS, 64.0, WIDTH_BLOCKS);

        assertEquals(SKEW_BLOCKS + HALF - 1.0E-5F, clamped.x);
        assertEquals(WIDTH_BLOCKS, clamped.z);
    }

    @Test
    void theSkewedWallStandsOnTheLatticeCopies() {
        assertTrue(SKEWED.getWallShifts().contains(new Vec3(SKEW_BLOCKS, 0.0, WIDTH_BLOCKS)));
        assertTrue(SKEWED.getWallShifts().contains(new Vec3(-SKEW_BLOCKS, 0.0, -WIDTH_BLOCKS)));
        assertTrue(SKEWED.getWallShifts().contains(new Vec3(WIDTH_BLOCKS, 0.0, 0.0)));
        assertEquals(9, SKEWED.getWallShifts().size());
    }

    @Test
    void onALongSkewedWorldTheEdgeIsMeasuredOnTheBestCopy() {
        TranslationLattice lattice = WorldFolds.of(
                FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-512, 512, -8, 8), LONG_SKEW_CHUNKS)).getBlockLattice();
        SeamBorder border = new SeamBorder(lattice, LONG_CENTER - HALF, LONG_CENTER + HALF, -HALF, HALF);
        Random random = new Random(LONG_SEED);
        for (int sample = 0; sample < LONG_SAMPLES; sample++) {
            double x = random.nextInt(-lattice.getX().domainLength, lattice.getX().domainLength) + 0.5;
            double z = random.nextInt(-3 * lattice.getZ().domainLength, 3 * lattice.getZ().domainLength) + 0.5;
            assertEquals(deepestInside(lattice, x - LONG_CENTER, z), border.distanceToEdgeBlocks(x, z), "at " + x + ", " + z);
        }
    }

    private static double deepestInside(TranslationLattice lattice, double deltaX, double deltaZ) {
        int widthX = lattice.getX().domainLength;
        int widthZ = lattice.getZ().domainLength;
        long center = Math.round(deltaZ / widthZ);
        long reachLaps = widthX / widthZ + 2L;
        double deepest = Double.NEGATIVE_INFINITY;
        for (long laps = center - reachLaps; laps <= center + reachLaps; laps++) {
            double offsetZ = deltaZ - (double) laps * widthZ;
            double x = deltaX - (double) laps * lattice.getSkew();
            double lapped = x - Math.floor(x / widthX) * widthX;
            double offsetX = Math.min(lapped, widthX - lapped);
            deepest = Math.max(deepest, Math.min(HALF - offsetX, HALF - Math.abs(offsetZ)));
        }

        return deepest;
    }
}
