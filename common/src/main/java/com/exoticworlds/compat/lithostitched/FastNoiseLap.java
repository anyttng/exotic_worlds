package com.exoticworlds.compat.lithostitched;

import com.google.common.math.IntMath;

public final class FastNoiseLap {
    public static final int OPEN = 0;

    static final int ROTATED_STEP = 3;

    private static final int AXIS_STEP = 1;

    private static final long ROTATED_NORM = 9L;

    private static final long TWO = 2L;

    private static final double CELL_EPSILON = 1.0E-9;

    private static final int INVERSE_NEWTON_STEPS = 5;

    public record Laps(int xBlocks, int zBlocks, double xzScale) {
    }

    public record Field(int seed, float frequency, float lacunarity, int octaves, boolean rotated) {
    }

    public record Primes(int x, int y, int z) {
    }

    private final int seed;

    private final boolean rotated;

    private final long[] xCells;

    private final long[] zCells;

    private final float[] steps;

    private final double blockScale;

    private final double inputScale;

    private final Primes primes;

    private final Primes inverses;

    public static FastNoiseLap of(Laps laps, Field field, Primes primes) {
        return new FastNoiseLap(laps, field, primes);
    }

    private FastNoiseLap(Laps laps, Field field, Primes primes) {
        this.seed = field.seed();
        this.rotated = field.rotated();
        this.primes = primes;
        this.inverses = new Primes(inverseOfOdd(primes.x()), inverseOfOdd(primes.y()), inverseOfOdd(primes.z()));
        int common = commonLap(laps);
        int step = field.rotated() ? ROTATED_STEP : AXIS_STEP;
        double commonInput = common * Math.abs(laps.xzScale());
        long[] counts = new long[field.octaves()];
        double rate = field.frequency();
        for (int octave = 0; octave < counts.length; octave++) {
            counts[octave] = Math.max(1L, (long) Math.ceil(commonInput * rate / step - CELL_EPSILON));
            rate *= field.lacunarity();
        }

        this.xCells = new long[counts.length];
        this.zCells = new long[counts.length];
        this.steps = new float[counts.length];
        for (int octave = 0; octave < counts.length; octave++) {
            this.xCells[octave] = cellsOf(laps.xBlocks(), common, step, counts[octave]);
            this.zCells[octave] = cellsOf(laps.zBlocks(), common, step, counts[octave]);
            this.steps[octave] = octave + 1 < counts.length
                    ? (float) ((double) counts[octave + 1] / counts[octave])
                    : field.lacunarity();
        }

        this.blockScale = (double) step * counts[0] / common;
        this.inputScale = this.blockScale / Math.abs(laps.xzScale());
    }

    public double blockScale() {
        return this.blockScale;
    }

    public double inputScale() {
        return this.inputScale;
    }

    public float step(int octave) {
        return octave >= 0 && octave < this.steps.length ? this.steps[octave] : this.steps[this.steps.length - 1];
    }

    public int reduceX(int octaveSeed, int xPrimed, int yPrimed, int zPrimed) {
        int octave = octaveSeed - this.seed;
        if (!this.reduces(octave)) {
            return xPrimed;
        }

        long x = unprimed(xPrimed, this.inverses.x());
        if (!this.rotated) {
            return primed(this.xCells[octave] == OPEN ? x : Math.floorMod(x, this.xCells[octave]), this.primes.x());
        }

        long y = unprimed(yPrimed, this.inverses.y());
        long z = unprimed(zPrimed, this.inverses.z());
        return primed(x + this.xLaps(octave, x, y, z) * this.xStep(octave)
                - TWO * this.zLaps(octave, x, y, z) * this.zStep(octave), this.primes.x());
    }

    public int reduceY(int octaveSeed, int xPrimed, int yPrimed, int zPrimed) {
        int octave = octaveSeed - this.seed;
        if (!this.rotated || !this.reduces(octave)) {
            return yPrimed;
        }

        long x = unprimed(xPrimed, this.inverses.x());
        long y = unprimed(yPrimed, this.inverses.y());
        long z = unprimed(zPrimed, this.inverses.z());
        return primed(y - TWO * this.xLaps(octave, x, y, z) * this.xStep(octave)
                - TWO * this.zLaps(octave, x, y, z) * this.zStep(octave), this.primes.y());
    }

    public int reduceZ(int octaveSeed, int xPrimed, int yPrimed, int zPrimed) {
        int octave = octaveSeed - this.seed;
        if (!this.reduces(octave)) {
            return zPrimed;
        }

        long z = unprimed(zPrimed, this.inverses.z());
        if (!this.rotated) {
            return primed(this.zCells[octave] == OPEN ? z : Math.floorMod(z, this.zCells[octave]), this.primes.z());
        }

        long x = unprimed(xPrimed, this.inverses.x());
        long y = unprimed(yPrimed, this.inverses.y());
        return primed(z - TWO * this.xLaps(octave, x, y, z) * this.xStep(octave)
                + this.zLaps(octave, x, y, z) * this.zStep(octave), this.primes.z());
    }

    long xCells(int octave) {
        return this.xCells[octave];
    }

    long zCells(int octave) {
        return this.zCells[octave];
    }

    private boolean reduces(int octave) {
        return octave >= 0 && octave < this.xCells.length;
    }

    private long xStep(int octave) {
        return this.xCells[octave] / ROTATED_STEP;
    }

    private long zStep(int octave) {
        return this.zCells[octave] / ROTATED_STEP;
    }

    private long xLaps(int octave, long x, long y, long z) {
        long step = this.xStep(octave);
        return step == OPEN ? 0L : Math.floorDiv(-x + TWO * y + TWO * z, ROTATED_NORM * step);
    }

    private long zLaps(int octave, long x, long y, long z) {
        long step = this.zStep(octave);
        return step == OPEN ? 0L : Math.floorDiv(TWO * x + TWO * y - z, ROTATED_NORM * step);
    }

    private static int commonLap(Laps laps) {
        if (laps.xBlocks() == OPEN) {
            return laps.zBlocks();
        }

        return laps.zBlocks() == OPEN ? laps.xBlocks() : IntMath.gcd(laps.xBlocks(), laps.zBlocks());
    }

    private static long cellsOf(int lapBlocks, int common, int step, long count) {
        return lapBlocks == OPEN ? OPEN : (long) (lapBlocks / common) * step * count;
    }

    private static int unprimed(int primed, int inverse) {
        return primed * inverse;
    }

    private static int primed(long index, int prime) {
        return (int) index * prime;
    }

    private static int inverseOfOdd(int odd) {
        int inverse = odd;
        for (int step = 0; step < INVERSE_NEWTON_STEPS; step++) {
            inverse *= 2 - odd * inverse;
        }

        return inverse;
    }
}
