package com.exoticworlds.compat.lithostitched;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FastNoiseLapTest {
    private static final FastNoiseLap.Primes PRIMES = new FastNoiseLap.Primes(0x2545F491, 0x6C8E9CF5, 0x7FEB352D);

    private static final int SEED = 4242;

    private static final int LAP_BLOCKS = 512;

    private static final int WIDE_LAP_BLOCKS = 768;

    private static final double XZ_SCALE = 1.0;

    private static final float FREQUENCY = 0.0047F;

    private static final float LACUNARITY = 2.1F;

    private static final int OCTAVES = 3;

    private static final int SINGLE_OCTAVE = 1;

    private static final int ROTATED_STEP = 3;

    private static final int NO_PERIOD = 0;

    private static final int SEARCH_LIMIT = 10_000;

    private static final int[][] POINTS = {{0, 0, 0}, {5, -3, 11}, {-17, 40, -9}, {123, 7, -301}, {-1, -1, -1}};

    private static final int[] X_AXIS = {1, 0, 0};

    private static final int[] Z_AXIS = {0, 0, 1};

    private static final int[] X_LAP_DIRECTION = {-1, 2, 2};

    private static final int[] Z_LAP_DIRECTION = {2, 2, -1};

    private static final double SCALE_TOLERANCE = 1.0E-5;

    private static FastNoiseLap lap(int xBlocks, int zBlocks, boolean rotated, int octaves) {
        return FastNoiseLap.of(new FastNoiseLap.Laps(xBlocks, zBlocks, XZ_SCALE),
                new FastNoiseLap.Field(SEED, FREQUENCY, LACUNARITY, octaves, rotated), PRIMES);
    }

    private static boolean sameReduction(FastNoiseLap lap, int octave, int[] point, int[] direction, int steps) {
        int seed = SEED + octave;
        int x = point[0] * PRIMES.x();
        int y = point[1] * PRIMES.y();
        int z = point[2] * PRIMES.z();
        int shiftedX = (point[0] + direction[0] * steps) * PRIMES.x();
        int shiftedY = (point[1] + direction[1] * steps) * PRIMES.y();
        int shiftedZ = (point[2] + direction[2] * steps) * PRIMES.z();
        return lap.reduceX(seed, x, y, z) == lap.reduceX(seed, shiftedX, shiftedY, shiftedZ)
                && lap.reduceY(seed, x, y, z) == lap.reduceY(seed, shiftedX, shiftedY, shiftedZ)
                && lap.reduceZ(seed, x, y, z) == lap.reduceZ(seed, shiftedX, shiftedY, shiftedZ);
    }

    private static int period(FastNoiseLap lap, int octave, int[] direction) {
        for (int steps = 1; steps <= SEARCH_LIMIT; steps++) {
            boolean repeats = true;
            for (int[] point : POINTS) {
                repeats &= sameReduction(lap, octave, point, direction, steps);
            }

            if (repeats) {
                return steps;
            }
        }

        return NO_PERIOD;
    }

    @Test
    void anAxisLatticeRepeatsOnUnevenLapsInProportionAndNoWiderThanTheMod() {
        FastNoiseLap lap = lap(LAP_BLOCKS, WIDE_LAP_BLOCKS, false, SINGLE_OCTAVE);
        int xCells = period(lap, 0, X_AXIS);
        int zCells = period(lap, 0, Z_AXIS);
        assertTrue(xCells >= LAP_BLOCKS * FREQUENCY);
        assertTrue(zCells >= WIDE_LAP_BLOCKS * FREQUENCY);
        assertEquals(LAP_BLOCKS * zCells, WIDE_LAP_BLOCKS * xCells);
    }

    @Test
    void aRotatedLatticeRepeatsAlongBothLapVectorsInEveryOctaveNoWiderThanTheMod() {
        FastNoiseLap lap = lap(LAP_BLOCKS, WIDE_LAP_BLOCKS, true, OCTAVES);
        double rate = FREQUENCY;
        for (int octave = 0; octave < OCTAVES; octave++) {
            int xCells = ROTATED_STEP * period(lap, octave, X_LAP_DIRECTION);
            int zCells = ROTATED_STEP * period(lap, octave, Z_LAP_DIRECTION);
            assertTrue(xCells >= LAP_BLOCKS * rate);
            assertTrue(zCells >= WIDE_LAP_BLOCKS * rate);
            assertEquals(LAP_BLOCKS * zCells, WIDE_LAP_BLOCKS * xCells);
            rate *= LACUNARITY;
        }
    }

    @Test
    void anOpenAxisKeepsItsIndices() {
        FastNoiseLap lap = lap(LAP_BLOCKS, FastNoiseLap.OPEN, false, SINGLE_OCTAVE);
        int z = 77 * PRIMES.z();
        assertEquals(z, lap.reduceZ(SEED, 3 * PRIMES.x(), 4 * PRIMES.y(), z));
        assertEquals(NO_PERIOD, period(lap, 0, Z_AXIS));
    }

    @Test
    void aSeedOutsideTheOctavesIsLeftAlone() {
        FastNoiseLap lap = lap(LAP_BLOCKS, LAP_BLOCKS, true, OCTAVES);
        int x = 9 * PRIMES.x();
        assertEquals(x, lap.reduceX(SEED + OCTAVES, x, 2 * PRIMES.y(), PRIMES.z()));
        assertEquals(x, lap.reduceX(SEED - 1, x, 2 * PRIMES.y(), PRIMES.z()));
    }

    @Test
    void theOctaveStepsCarryTheScaleOntoEachOctavesWholeCells() {
        FastNoiseLap lap = lap(LAP_BLOCKS, LAP_BLOCKS, true, OCTAVES);
        double scale = lap.blockScale();
        for (int octave = 0; octave < OCTAVES; octave++) {
            double cells = ROTATED_STEP * period(lap, octave, X_LAP_DIRECTION);
            assertEquals(cells, scale * LAP_BLOCKS, cells * SCALE_TOLERANCE);
            scale *= lap.step(octave);
        }
    }
}
