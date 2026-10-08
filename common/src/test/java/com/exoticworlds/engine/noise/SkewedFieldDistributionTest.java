package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.BlendedNoiseFixture.mix;
import static com.exoticworlds.engine.noise.BlendedNoiseFixture.sample;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.BlendedNoiseFixture.Octave;

class SkewedFieldDistributionTest {
    private static final WorldLoopBounds BOUNDS = new WorldLoopBounds(-16, 16, -16, 16);
    private static final int WORLD_BLOCKS = 512;
    private static final double HALF_WORLD = 256.0;
    private static final int[] SKEW_CHUNKS = {5, 16};

    private static final double[] FRACTIONS = {
            0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.4375};

    private static final int SEEDS = 1024;
    private static final int GRID = 32;
    private static final double WINDOW_SPAN = 1.0E6;
    private static final long WINDOW_SEED = 0x0153EL;
    private static final long OCTAVE_SEED = 0xCA11B7A7EL;
    private static final long OCTAVE_SEED_STEP = 7919L;

    private static final double HORIZONTAL_SHARE = 0.0;
    private static final double RMS_BAND = 0.06;
    private static final double MEAN_SPREAD_MIN = 0.7;
    private static final double MEAN_SPREAD_MAX = 1.3;

    private static final Path REPORT = Path.of(System.getProperty("toroidal.reports", "build/reports"))
            .resolve("skewed-field-distribution.txt");

    private record Reading(String world, double fraction, double rmsRatio, double meanSpreadRatio) {
    }

    @Test
    void aDampedOctaveKeepsTheVanillaWindowOnASkewedLattice() {
        List<Reading> readings = new ArrayList<>();
        readings.addAll(measure("torus", WorldFolds.of(FlatShape.torus(BOUNDS))));
        for (int skewChunks : SKEW_CHUNKS) {
            readings.addAll(measure("skew " + skewChunks, new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, skewChunks))));
        }

        StringBuilder report = new StringBuilder("Damped-octave gates, 512-block lap, ")
                .append(SEEDS).append(" seeds x ").append(GRID).append("x").append(GRID).append(" grid\n");
        for (Reading reading : readings) {
            report.append(String.format("  %-8s f=%.4f rms corrected/vanilla %.5f, mean spread corrected/vanilla %.5f%n",
                    reading.world(), reading.fraction(), reading.rmsRatio(), reading.meanSpreadRatio()));
        }

        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (Reading reading : readings) {
            assertTrue(Math.abs(reading.rmsRatio() - 1.0) <= RMS_BAND,
                    () -> reading.world() + " damped octave rms drifted off the vanilla window at f="
                            + reading.fraction() + ": " + reading.rmsRatio());
            assertTrue(reading.meanSpreadRatio() > MEAN_SPREAD_MIN && reading.meanSpreadRatio() < MEAN_SPREAD_MAX,
                    () -> reading.world() + " world-mean spread drifted off vanilla at f=" + reading.fraction()
                            + ": " + reading.meanSpreadRatio());
        }
    }

    @SuppressWarnings("deprecation")
    private static List<Reading> measure(String world, WorldFold fold) {
        List<Reading> readings = new ArrayList<>();
        for (double fraction : FRACTIONS) {
            double scale = fraction / WORLD_BLOCKS;
            Random windows = new Random(WINDOW_SEED);
            double vanillaVariance = 0.0;
            double correctedVariance = 0.0;
            double[] vanillaMeans = new double[SEEDS];
            double[] correctedMeans = new double[SEEDS];
            for (int s = 0; s < SEEDS; s++) {
                Octave octave = Octave.of(mix(OCTAVE_SEED + s * OCTAVE_SEED_STEP));
                double windowX = windows.nextDouble() * WINDOW_SPAN;
                double windowZ = windows.nextDouble() * WINDOW_SPAN;
                double[] vanillaGrid = new double[GRID * GRID];
                double[] correctedGrid = new double[GRID * GRID];
                int k = 0;
                for (int i = 0; i < GRID; i++) {
                    double x = i * (WORLD_BLOCKS / (double) GRID);
                    for (int j = 0; j < GRID; j++) {
                        double z = j * (WORLD_BLOCKS / (double) GRID);
                        vanillaGrid[k] = octave.vanilla().noise((windowX + x) * scale, 0.0, (windowZ + z) * scale);
                        correctedGrid[k] = sample(octave.permutations(), octave.xo(), octave.yo(), octave.zo(), fold,
                                scale, x - HALF_WORLD, 0.0, z - HALF_WORLD, 0.0, 0.0, HORIZONTAL_SHARE);
                        k++;
                    }
                }
                vanillaVariance += variance(vanillaGrid);
                correctedVariance += variance(correctedGrid);
                vanillaMeans[s] = mean(vanillaGrid);
                correctedMeans[s] = mean(correctedGrid);
            }

            readings.add(new Reading(world, fraction, Math.sqrt(correctedVariance / vanillaVariance),
                    Math.sqrt(variance(correctedMeans) / variance(vanillaMeans))));
        }

        return readings;
    }

    private static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        return sum / values.length;
    }

    private static double variance(double[] values) {
        double mean = mean(values);
        double sum = 0.0;
        for (double value : values) {
            sum += (value - mean) * (value - mean);
        }

        return sum / values.length;
    }
}
