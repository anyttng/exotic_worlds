package com.exoticworlds.compat.reterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import raccoonman.reterraforged.world.worldgen.noise.module.Simplex;
import raccoonman.reterraforged.world.worldgen.noise.module.Simplex2;

class SimplexOnSquareTest {
    private static final long SAMPLE_SEED = 0x51A7L;

    private static final int NOISE_SEED = 1337;

    private static final int SAMPLES = 200_000;

    private static final int LAG_SAMPLES = 40_000;

    private static final double SPREAD = 4096.0;

    private static final double LAG_STEP = 0.005;

    private static final double MAX_LAG = 3.0;

    private static final double HALF = 0.5;

    private static final double TOLERANCE = 0.02;

    private interface Field {
        float at(float x, float z);
    }

    private static final Field SIMPLEX = (x, z) -> Simplex.sample(x, z, NOISE_SEED);

    private static final Field SIMPLEX2 = (x, z) -> Simplex2.sample(x, z, NOISE_SEED);

    private static final Field SQUARE = (x, z) -> SimplexOnSquare.sample(x, z, NOISE_SEED, 1.0F);

    @Test
    void theSquareLatticeMatchesSimplexInAmplitudeAndFeatureSize() {
        double simplexSpread = deviation(SIMPLEX);
        double simplex2Spread = deviation(SIMPLEX2);
        double squareSpread = deviation(SQUARE);
        double rateX = halfLag(SQUARE, true) / halfLag(SIMPLEX, true);
        double rateZ = halfLag(SQUARE, false) / halfLag(SIMPLEX, false);
        String measured = "rate x " + rateX + ", rate z " + rateZ + ", simplex amplitude " + simplexSpread / squareSpread
                + ", simplex2 amplitude " + simplex2Spread / squareSpread;

        assertEquals(SimplexOnSquare.RATE, rateX, TOLERANCE * SimplexOnSquare.RATE, measured);
        assertEquals(SimplexOnSquare.RATE, rateZ, TOLERANCE * SimplexOnSquare.RATE, measured);
        assertEquals(SimplexOnSquare.SIMPLEX_AMPLITUDE, simplexSpread / squareSpread,
                TOLERANCE * SimplexOnSquare.SIMPLEX_AMPLITUDE, measured);
        assertEquals(SimplexOnSquare.SIMPLEX2_AMPLITUDE, simplex2Spread / squareSpread,
                TOLERANCE * SimplexOnSquare.SIMPLEX2_AMPLITUDE, measured);
    }

    private static double deviation(Field field) {
        Random random = new Random(SAMPLE_SEED);
        double sum = 0.0;
        double sumOfSquares = 0.0;
        for (int i = 0; i < SAMPLES; i++) {
            double value = field.at((float) (random.nextDouble() * SPREAD), (float) (random.nextDouble() * SPREAD));
            sum += value;
            sumOfSquares += value * value;
        }

        double mean = sum / SAMPLES;
        return Math.sqrt(sumOfSquares / SAMPLES - mean * mean);
    }

    private static double halfLag(Field field, boolean alongX) {
        Random random = new Random(SAMPLE_SEED);
        float[] xs = new float[LAG_SAMPLES];
        float[] zs = new float[LAG_SAMPLES];
        double[] base = new double[LAG_SAMPLES];
        double sum = 0.0;
        for (int i = 0; i < LAG_SAMPLES; i++) {
            xs[i] = (float) (random.nextDouble() * SPREAD);
            zs[i] = (float) (random.nextDouble() * SPREAD);
            base[i] = field.at(xs[i], zs[i]);
            sum += base[i];
        }

        double mean = sum / LAG_SAMPLES;
        double variance = 0.0;
        for (double value : base) {
            variance += (value - mean) * (value - mean);
        }

        double previousLag = 0.0;
        double previousCorrelation = 1.0;
        for (double lag = LAG_STEP; lag <= MAX_LAG; lag += LAG_STEP) {
            double covariance = 0.0;
            for (int i = 0; i < LAG_SAMPLES; i++) {
                float x = alongX ? (float) (xs[i] + lag) : xs[i];
                float z = alongX ? zs[i] : (float) (zs[i] + lag);
                covariance += (base[i] - mean) * (field.at(x, z) - mean);
            }

            double correlation = covariance / variance;
            if (correlation <= HALF) {
                return previousLag + (lag - previousLag) * (previousCorrelation - HALF)
                        / (previousCorrelation - correlation);
            }

            previousLag = lag;
            previousCorrelation = correlation;
        }

        throw new AssertionError("the correlation never fell to one half within " + MAX_LAG + " cells");
    }
}
