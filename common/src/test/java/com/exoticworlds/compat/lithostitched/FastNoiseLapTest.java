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

    private static final int[][] POINTS = {{0, 0, 0}, {5, -3, 11}, {-17, 40, -9}, {123, 7, -301}, {-1, -1, -1}};

    private static final double SCALE_TOLERANCE = 1.0E-5;

    private static FastNoiseLap lap(int xBlocks, int zBlocks, boolean rotated, int octaves) {
        return FastNoiseLap.of(new FastNoiseLap.Laps(xBlocks, zBlocks, XZ_SCALE),
                new FastNoiseLap.Field(SEED, FREQUENCY, LACUNARITY, octaves, rotated), PRIMES);
    }

    private static int primed(int index, int prime) {
        return index * prime;
    }

    private static void assertSameReduction(FastNoiseLap lap, int octave, int[] point, int[] shifted) {
        int seed = SEED + octave;
        int x = primed(point[0], PRIMES.x());
        int y = primed(point[1], PRIMES.y());
        int z = primed(point[2], PRIMES.z());
        int shiftedX = primed(shifted[0], PRIMES.x());
        int shiftedY = primed(shifted[1], PRIMES.y());
        int shiftedZ = primed(shifted[2], PRIMES.z());
        assertEquals(lap.reduceX(seed, x, y, z), lap.reduceX(seed, shiftedX, shiftedY, shiftedZ));
        assertEquals(lap.reduceY(seed, x, y, z), lap.reduceY(seed, shiftedX, shiftedY, shiftedZ));
        assertEquals(lap.reduceZ(seed, x, y, z), lap.reduceZ(seed, shiftedX, shiftedY, shiftedZ));
    }

    @Test
    void anAxisLatticeRepeatsOneLapAlongEachLoopingAxis() {
        FastNoiseLap lap = lap(LAP_BLOCKS, WIDE_LAP_BLOCKS, false, SINGLE_OCTAVE);
        int xCells = (int) lap.xCells(0);
        int zCells = (int) lap.zCells(0);
        for (int[] point : POINTS) {
            assertSameReduction(lap, 0, point, new int[] {point[0] + xCells, point[1], point[2]});
            assertSameReduction(lap, 0, point, new int[] {point[0], point[1], point[2] - zCells});
        }
    }

    @Test
    void aRotatedLatticeRepeatsAlongBothLapVectorsInEveryOctave() {
        FastNoiseLap lap = lap(LAP_BLOCKS, WIDE_LAP_BLOCKS, true, OCTAVES);
        for (int octave = 0; octave < OCTAVES; octave++) {
            int a = (int) (lap.xCells(octave) / FastNoiseLap.ROTATED_STEP);
            int b = (int) (lap.zCells(octave) / FastNoiseLap.ROTATED_STEP);
            for (int[] point : POINTS) {
                assertSameReduction(lap, octave, point, new int[] {point[0] - a, point[1] + 2 * a, point[2] + 2 * a});
                assertSameReduction(lap, octave, point,
                        new int[] {point[0] + 2 * b, point[1] + 2 * b, point[2] - b});
            }
        }
    }

    @Test
    void anOpenAxisKeepsItsIndices() {
        FastNoiseLap lap = lap(LAP_BLOCKS, FastNoiseLap.OPEN, false, SINGLE_OCTAVE);
        int z = primed(77, PRIMES.z());
        assertEquals(z, lap.reduceZ(SEED, primed(3, PRIMES.x()), primed(4, PRIMES.y()), z));
        assertEquals(FastNoiseLap.OPEN, lap.zCells(0));
    }

    @Test
    void aSeedOutsideTheOctavesIsLeftAlone() {
        FastNoiseLap lap = lap(LAP_BLOCKS, LAP_BLOCKS, true, OCTAVES);
        int x = primed(9, PRIMES.x());
        assertEquals(x, lap.reduceX(SEED + OCTAVES, x, primed(2, PRIMES.y()), primed(1, PRIMES.z())));
        assertEquals(x, lap.reduceX(SEED - 1, x, primed(2, PRIMES.y()), primed(1, PRIMES.z())));
    }

    @Test
    void noOctaveIsWiderThanTheModsOwnAndARotatedLapHoldsWholeSteps() {
        FastNoiseLap lap = lap(LAP_BLOCKS, WIDE_LAP_BLOCKS, true, OCTAVES);
        double rate = FREQUENCY;
        for (int octave = 0; octave < OCTAVES; octave++) {
            assertTrue(lap.xCells(octave) >= LAP_BLOCKS * rate);
            assertTrue(lap.zCells(octave) >= WIDE_LAP_BLOCKS * rate);
            assertEquals(0L, lap.xCells(octave) % FastNoiseLap.ROTATED_STEP);
            assertEquals(0L, lap.zCells(octave) % FastNoiseLap.ROTATED_STEP);
            assertEquals((double) LAP_BLOCKS / WIDE_LAP_BLOCKS, (double) lap.xCells(octave) / lap.zCells(octave));
            rate *= LACUNARITY;
        }
    }

    @Test
    void theOctaveStepsCarryTheScaleOntoEachOctavesWholeCells() {
        FastNoiseLap lap = lap(LAP_BLOCKS, LAP_BLOCKS, true, OCTAVES);
        double scale = lap.blockScale();
        for (int octave = 0; octave < OCTAVES; octave++) {
            double cells = lap.xCells(octave);
            assertEquals(cells, scale * LAP_BLOCKS, cells * SCALE_TOLERANCE);
            scale *= lap.step(octave);
        }
    }
}
