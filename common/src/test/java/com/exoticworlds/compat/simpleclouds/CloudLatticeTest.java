package com.exoticworlds.compat.simpleclouds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class CloudLatticeTest {
    private static final long SEED = 850L;
    private static final int SAMPLES = 400;
    private static final float TINY_LAP = 64.0F;
    private static final float LARGE_LAP = 512.0F;
    private static final float MIN_STRETCH = 0.1F;
    private static final double RELATIVE_TOLERANCE = 1.0E-4;

    @Test
    void anUnwrappedWorldLeavesTheQueryWhereItIs() {
        CloudLattice lattice = CloudLattice.of(0.0F, 0.0F, 0.5F, 0.3F, -0.2F, 1.0F);
        assertEquals(CloudLattice.NONE, lattice, "no looped axis still built a lattice");
        assertEquals(1000.25F, lattice.seatedX(3.0F, 4.0F, 0.5F, 0.3F, -0.2F, 1.0F, 1000.25F, -700.5F), 0.0F,
                "the unwrapped world moved the query along X");
        assertEquals(-700.5F, lattice.seatedZ(3.0F, 4.0F, 0.5F, 0.3F, -0.2F, 1.0F, 1000.25F, -700.5F), 0.0F,
                "the unwrapped world moved the query along Z");
    }

    @Test
    void aTorusSeatsTheQueryOnTheCopyNearestTheRegionInItsOwnMetric() {
        Random random = new Random(SEED);
        for (int sample = 0; sample < SAMPLES; sample++) {
            float lapX = sample % 2 == 0 ? TINY_LAP : LARGE_LAP;
            float lapZ = sample % 3 == 0 ? TINY_LAP : LARGE_LAP;
            assertSeatsNearest(random, lapX, lapZ);
        }
    }

    @Test
    void aCylinderSeatsTheQueryAlongItsLoopedAxisAlone() {
        Random random = new Random(SEED + 1);
        for (int sample = 0; sample < SAMPLES; sample++) {
            assertSeatsNearest(random, sample % 2 == 0 ? TINY_LAP : 0.0F, sample % 2 == 0 ? 0.0F : TINY_LAP);
        }
    }

    private static void assertSeatsNearest(Random random, float lapX, float lapZ) {
        float stretch = MIN_STRETCH + random.nextFloat() * (1.0F - MIN_STRETCH);
        double rotation = random.nextDouble() * 2.0 * Math.PI;
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        float m00 = stretch * cos;
        float m01 = sin;
        float m10 = -stretch * sin;
        float m11 = cos;
        float posX = (random.nextFloat() - 0.5F) * 4.0F * LARGE_LAP;
        float posZ = (random.nextFloat() - 0.5F) * 4.0F * LARGE_LAP;
        float x = (float) Math.floor(posX + (random.nextFloat() - 0.5F) * 8.0F * LARGE_LAP) + 0.0625F;
        float z = (float) Math.floor(posZ + (random.nextFloat() - 0.5F) * 8.0F * LARGE_LAP) + 0.5F;

        CloudLattice lattice = CloudLattice.of(lapX, lapZ, m00, m01, m10, m11);
        float seatedX = lattice.seatedX(posX, posZ, m00, m01, m10, m11, x, z);
        float seatedZ = lattice.seatedZ(posX, posZ, m00, m01, m10, m11, x, z);
        String context = "laps " + lapX + " x " + lapZ + ", stretch " + stretch + ", rotation " + rotation
                + ", region (" + posX + ", " + posZ + "), query (" + x + ", " + z + ")";

        assertWholeLaps(seatedX - x, lapX, "X", context);
        assertWholeLaps(seatedZ - z, lapZ, "Z", context);
        double expected = bruteForceNearest(lapX, lapZ, m00, m01, m10, m11, posX - x, posZ - z, stretch);
        double actual = image(seatedX - posX, seatedZ - posZ, m00, m01, m10, m11);
        assertEquals(expected, actual, Math.max(RELATIVE_TOLERANCE, expected * RELATIVE_TOLERANCE),
                "the seat is not the nearest copy in the region's metric: " + context);

        assertEquals(seatedZ, lattice.seatedZ(posX, posZ, m00, m01, m10, m11, seatedX, z), 0.0F,
                "the Z read picked another copy once X was seated: " + context);
        assertEquals(seatedX, lattice.seatedX(posX, posZ, m00, m01, m10, m11, x, seatedZ), 0.0F,
                "the X read picked another copy once Z was seated: " + context);
    }

    private static void assertWholeLaps(float moved, float lap, String axis, String context) {
        if (lap == 0.0F) {
            assertEquals(0.0F, moved, 0.0F, "the unbounded " + axis + " axis moved: " + context);
            return;
        }

        double laps = moved / lap;
        assertTrue(Math.abs(laps - Math.rint(laps)) < 1.0E-6, axis + " moved " + moved + ", not whole laps of "
                + lap + ": " + context);
    }

    private static double bruteForceNearest(float lapX, float lapZ, float m00, float m01, float m10, float m11,
            float towardRegionX, float towardRegionZ, float stretch) {
        long centreX = lapX == 0.0F ? 0 : Math.round(towardRegionX / (double) lapX);
        long centreZ = lapZ == 0.0F ? 0 : Math.round(towardRegionZ / (double) lapZ);
        double shortestLap = Math.min(lapX == 0.0F ? Double.MAX_VALUE : lapX, lapZ == 0.0F ? Double.MAX_VALUE : lapZ);
        boolean torus = lapX != 0.0F && lapZ != 0.0F;
        double within = torus ? Math.hypot(lapX, lapZ) : Math.hypot(towardRegionX, towardRegionZ);
        int reach = (int) Math.ceil(within * (1.0 + 1.0 / stretch) / shortestLap) + 2;
        int reachX = lapX == 0.0F ? 0 : reach;
        int reachZ = lapZ == 0.0F ? 0 : reach;
        double best = Double.MAX_VALUE;
        for (long a = centreX - reachX; a <= centreX + reachX; a++) {
            for (long b = centreZ - reachZ; b <= centreZ + reachZ; b++) {
                best = Math.min(best, image(a * (double) lapX - towardRegionX, b * (double) lapZ - towardRegionZ,
                        m00, m01, m10, m11));
            }
        }

        return best;
    }

    private static double image(double x, double z, float m00, float m01, float m10, float m11) {
        double tx = m00 * x + m10 * z;
        double tz = m01 * x + m11 * z;
        return tx * tx + tz * tz;
    }
}
