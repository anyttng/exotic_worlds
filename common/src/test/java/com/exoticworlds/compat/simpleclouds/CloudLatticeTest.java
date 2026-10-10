package com.exoticworlds.compat.simpleclouds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec2;

class CloudLatticeTest {
    private static final long SEED = 850L;
    private static final int SAMPLES = 400;
    private static final float TINY_LAP = 64.0F;
    private static final float LARGE_LAP = 512.0F;
    private static final float NO_SKEW = 0.0F;
    private static final float CLOUD_UNITS_PER_CHUNK = 2.0F;
    private static final float MIN_STRETCH = 0.1F;
    private static final double RELATIVE_TOLERANCE = 1.0E-4;
    private static final double WHOLE_TOLERANCE = 1.0E-6;

    @Test
    void anUnwrappedWorldLeavesTheQueryWhereItIs() {
        CloudLattice lattice = CloudLattice.of(0.0F, 0.0F, NO_SKEW, 0.5F, 0.3F, -0.2F, 1.0F);
        assertEquals(CloudLattice.NONE, lattice, "no looped axis still built a lattice");
        Vec2 seated = lattice.seated(3.0F, 4.0F, 0.5F, 0.3F, -0.2F, 1.0F, 1000.25F, -700.5F);
        assertEquals(1000.25F, seated.x, 0.0F, "the unwrapped world moved the query along X");
        assertEquals(-700.5F, seated.y, 0.0F, "the unwrapped world moved the query along Z");
    }

    @Test
    void aTorusSeatsTheQueryOnTheCopyNearestTheRegionInItsOwnMetric() {
        Random random = new Random(SEED);
        for (int sample = 0; sample < SAMPLES; sample++) {
            float lapX = sample % 2 == 0 ? TINY_LAP : LARGE_LAP;
            float lapZ = sample % 3 == 0 ? TINY_LAP : LARGE_LAP;
            assertSeatsNearest(random, lapX, lapZ, NO_SKEW);
        }
    }

    @Test
    void aCylinderSeatsTheQueryAlongItsLoopedAxisAlone() {
        Random random = new Random(SEED + 1);
        for (int sample = 0; sample < SAMPLES; sample++) {
            assertSeatsNearest(random, sample % 2 == 0 ? TINY_LAP : 0.0F, sample % 2 == 0 ? 0.0F : TINY_LAP, NO_SKEW);
        }
    }

    @Test
    void aLatticeTorusSeatsTheQueryOnTheCopyNearestTheRegionAcrossTheSkew() {
        Random random = new Random(SEED + 2);
        for (int sample = 0; sample < SAMPLES; sample++) {
            float lapX = sample % 2 == 0 ? TINY_LAP : LARGE_LAP;
            float lapZ = sample % 3 == 0 ? TINY_LAP : LARGE_LAP;
            int chunksAcross = (int) (lapX / CLOUD_UNITS_PER_CHUNK);
            float skew = (1 + random.nextInt(chunksAcross - 1)) * CLOUD_UNITS_PER_CHUNK;
            assertSeatsNearest(random, lapX, lapZ, skew);
        }
    }

    private static void assertSeatsNearest(Random random, float lapX, float lapZ, float skew) {
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

        CloudLattice lattice = CloudLattice.of(lapX, lapZ, skew, m00, m01, m10, m11);
        Vec2 seated = lattice.seated(posX, posZ, m00, m01, m10, m11, x, z);
        float seatedX = seated.x;
        float seatedZ = seated.y;
        String context = "laps " + lapX + " x " + lapZ + ", skew " + skew + ", stretch " + stretch + ", rotation "
                + rotation + ", region (" + posX + ", " + posZ + "), query (" + x + ", " + z + ")";

        assertOnLattice(seatedX - x, seatedZ - z, lapX, lapZ, skew, context);
        double expected = bruteForceNearest(lapX, lapZ, skew, m00, m01, m10, m11, posX - x, posZ - z, stretch);
        double actual = image(seatedX - posX, seatedZ - posZ, m00, m01, m10, m11);
        assertEquals(expected, actual, Math.max(RELATIVE_TOLERANCE, expected * RELATIVE_TOLERANCE),
                "the seat is not the nearest copy in the region's metric: " + context);

        Vec2 reseated = lattice.seated(posX, posZ, m00, m01, m10, m11, seatedX, seatedZ);
        assertEquals(seatedX, reseated.x, 0.0F, "seating the seated query moved it along X: " + context);
        assertEquals(seatedZ, reseated.y, 0.0F, "seating the seated query moved it along Z: " + context);
    }

    private static void assertOnLattice(float movedX, float movedZ, float lapX, float lapZ, float skew,
            String context) {
        double rows = lapZ == 0.0F ? 0.0 : movedZ / (double) lapZ;
        if (lapZ == 0.0F) {
            assertEquals(0.0F, movedZ, 0.0F, "the unbounded Z axis moved: " + context);
        } else {
            assertWhole(rows, "Z moved " + movedZ + ", not whole laps of " + lapZ + ": " + context);
        }

        double alongRow = movedX - Math.rint(rows) * skew;
        if (lapX == 0.0F) {
            assertEquals(0.0, alongRow, 0.0, "the unbounded X axis moved: " + context);
            return;
        }

        assertWhole(alongRow / lapX, "X moved " + movedX + ", not whole laps of " + lapX + " after " + Math.rint(rows)
                + " rows of skew " + skew + ": " + context);
    }

    private static void assertWhole(double laps, String message) {
        assertTrue(Math.abs(laps - Math.rint(laps)) < WHOLE_TOLERANCE, message);
    }

    private static double bruteForceNearest(float lapX, float lapZ, float skew, float m00, float m01, float m10,
            float m11, float towardRegionX, float towardRegionZ, float stretch) {
        long centreZ = lapZ == 0.0F ? 0 : Math.round(towardRegionZ / (double) lapZ);
        double shortestLap = Math.min(lapX == 0.0F ? Double.MAX_VALUE : lapX, lapZ == 0.0F ? Double.MAX_VALUE : lapZ);
        boolean torus = lapX != 0.0F && lapZ != 0.0F;
        double within = torus ? Math.hypot(lapX + Math.abs(skew), lapZ) : Math.hypot(towardRegionX, towardRegionZ);
        int reach = (int) Math.ceil(within * (1.0 + 1.0 / stretch) / shortestLap) + 2;
        int reachX = lapX == 0.0F ? 0 : reach;
        int reachZ = lapZ == 0.0F ? 0 : reach;
        double best = Double.MAX_VALUE;
        for (long b = centreZ - reachZ; b <= centreZ + reachZ; b++) {
            double rowShift = b * (double) skew;
            long centreX = lapX == 0.0F ? 0 : Math.round((towardRegionX - rowShift) / lapX);
            for (long a = centreX - reachX; a <= centreX + reachX; a++) {
                best = Math.min(best, image(a * (double) lapX + rowShift - towardRegionX,
                        b * (double) lapZ - towardRegionZ, m00, m01, m10, m11));
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
