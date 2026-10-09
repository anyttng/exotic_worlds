package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import org.betterx.wover.math.api.noise.OpenSimplexNoise;
import org.junit.jupiter.api.Test;

class OpenSimplexQuantilesTest {
    private static final long SEED = 0x937L;

    private static final long SHARE_SEED = 0x938L;

    private static final int FIELDS = 16;

    private static final int SAMPLES_PER_FIELD = 200000;

    private static final int SHARE_SAMPLES_PER_FIELD = 20000;

    private static final double ORIGIN_SPREAD = 1.0E4;

    private static final int EVEN_LEVELS = 32;

    private static final double[] TAIL_LEVELS = {0.99, 0.999, 0.9999};

    private static final double TABLE_ROUNDING = 5.0E-5;

    private static final double[] SURFACE_THRESHOLDS = {0.0, 0.15, 0.3, 0.4, 0.5, 0.6};

    private static final double SHARE_TOLERANCE = 0.005;

    private static final Path REPORT =
            Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("open-simplex-quantiles.txt");

    @SuppressWarnings("ArrayRecordComponent")
    private record Magnitudes(double[] openSimplex, double[] standIn) {
        double[] openSimplexKnots() {
            return knots(this.openSimplex);
        }

        double[] standInKnots() {
            return knots(this.standIn);
        }
    }

    @Test
    void theTablesAreTheMeasuredQuantilesOfBothForms() {
        Magnitudes flat = measure(false);
        Magnitudes volume = measure(true);
        String report = "levels " + Arrays.toString(levels()) + "\n"
                + "2D stand-in " + literal(flat.standInKnots()) + "\n"
                + "2D OpenSimplex " + literal(flat.openSimplexKnots()) + "\n"
                + "3D stand-in " + literal(volume.standInKnots()) + "\n"
                + "3D OpenSimplex " + literal(volume.openSimplexKnots()) + "\n";
        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, report);
        } catch (IOException unwritable) {
            throw new UncheckedIOException(unwritable);
        }

        assertTable(OpenSimplexQuantiles.STAND_IN_2D, flat.standInKnots(), report);
        assertTable(OpenSimplexQuantiles.OPEN_SIMPLEX_2D, flat.openSimplexKnots(), report);
        assertTable(OpenSimplexQuantiles.STAND_IN_3D, volume.standInKnots(), report);
        assertTable(OpenSimplexQuantiles.OPEN_SIMPLEX_3D, volume.openSimplexKnots(), report);
    }

    @Test
    void theMatchedStandInPassesEachSurfaceThresholdAsOftenAsOpenSimplex() {
        Random random = new Random(SHARE_SEED);
        long[][] above = new long[2][SURFACE_THRESHOLDS.length];
        long[][] above3d = new long[2][SURFACE_THRESHOLDS.length];
        long samples = 0;
        for (int field = 0; field < FIELDS; field++) {
            OpenSimplexNoise reference = new OpenSimplexNoise(random.nextLong());
            OpenSimplexStandIn candidate = new OpenSimplexStandIn(random.nextLong());
            for (int sample = 0; sample < SHARE_SAMPLES_PER_FIELD; sample++, samples++) {
                double x = random.nextDouble() * ORIGIN_SPREAD;
                double y = random.nextDouble() * ORIGIN_SPREAD;
                double z = random.nextDouble() * ORIGIN_SPREAD;
                count(above[0], reference.eval(x, z));
                count(above[1], OpenSimplexQuantiles.matched(
                        candidate.eval(x, z, OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED)));
                count(above3d[0], reference.eval(x, y, z));
                count(above3d[1], OpenSimplexQuantiles.matched3d(
                        candidate.eval(x, y, z, OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED)));
            }
        }

        StringBuilder readings = new StringBuilder("share above threshold, matched stand-in / OpenSimplex");
        for (int t = 0; t < SURFACE_THRESHOLDS.length; t++) {
            readings.append(String.format(Locale.ROOT, "; %.2f: 2D %.4f / %.4f, 3D %.4f / %.4f",
                    SURFACE_THRESHOLDS[t], (double) above[1][t] / samples, (double) above[0][t] / samples,
                    (double) above3d[1][t] / samples, (double) above3d[0][t] / samples));
        }

        for (int t = 0; t < SURFACE_THRESHOLDS.length; t++) {
            assertEquals((double) above[0][t] / samples, (double) above[1][t] / samples, SHARE_TOLERANCE,
                    readings.toString());
            assertEquals((double) above3d[0][t] / samples, (double) above3d[1][t] / samples, SHARE_TOLERANCE,
                    readings.toString());
        }
    }

    @Test
    void theMatchKeepsTheSignAndTheOrder() {
        double previous = OpenSimplexQuantiles.matched(-2.0);
        for (double value = -2.0; value <= 2.0; value += 0.001) {
            double matched = OpenSimplexQuantiles.matched(value);
            assertEquals(Math.signum(value), Math.signum(matched), "sign at " + value);
            assertTrue(matched >= previous, "order at " + value);
            assertEquals(-matched, OpenSimplexQuantiles.matched(-value), TABLE_ROUNDING, "symmetry at " + value);
            previous = matched;
        }
    }

    private static void assertTable(double[] table, double[] measured, String report) {
        assertEquals(measured.length + 1, table.length, report);
        assertEquals(0.0, table[0], report);
        for (int k = 0; k < measured.length; k++) {
            assertEquals(measured[k], table[k + 1], TABLE_ROUNDING, report);
        }
    }

    private static void count(long[] above, double value) {
        for (int t = 0; t < SURFACE_THRESHOLDS.length; t++) {
            if (value > SURFACE_THRESHOLDS[t]) {
                above[t]++;
            }
        }
    }

    private static Magnitudes measure(boolean volume) {
        Random random = new Random(SEED + (volume ? 1L : 0L));
        int total = FIELDS * SAMPLES_PER_FIELD;
        double[] openSimplex = new double[total];
        double[] standIn = new double[total];
        int at = 0;
        for (int field = 0; field < FIELDS; field++) {
            OpenSimplexNoise reference = new OpenSimplexNoise(random.nextLong());
            OpenSimplexStandIn candidate = new OpenSimplexStandIn(random.nextLong());
            for (int sample = 0; sample < SAMPLES_PER_FIELD; sample++, at++) {
                double x = random.nextDouble() * ORIGIN_SPREAD;
                double y = random.nextDouble() * ORIGIN_SPREAD;
                double z = random.nextDouble() * ORIGIN_SPREAD;
                openSimplex[at] = Math.abs(volume ? reference.eval(x, y, z) : reference.eval(x, z));
                standIn[at] = Math.abs(volume
                        ? candidate.eval(x, y, z, OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED)
                        : candidate.eval(x, z, OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED));
            }
        }

        Arrays.sort(openSimplex);
        Arrays.sort(standIn);
        return new Magnitudes(openSimplex, standIn);
    }

    private static double[] levels() {
        double[] levels = new double[EVEN_LEVELS - 1 + TAIL_LEVELS.length];
        for (int k = 1; k < EVEN_LEVELS; k++) {
            levels[k - 1] = (double) k / EVEN_LEVELS;
        }

        System.arraycopy(TAIL_LEVELS, 0, levels, EVEN_LEVELS - 1, TAIL_LEVELS.length);
        return levels;
    }

    private static double[] knots(double[] sorted) {
        double[] levels = levels();
        double[] knots = new double[levels.length];
        for (int k = 0; k < levels.length; k++) {
            knots[k] = sorted[(int) Math.min(sorted.length - 1, Math.round(levels[k] * (sorted.length - 1)))];
        }

        return knots;
    }

    private static String literal(double[] values) {
        StringBuilder text = new StringBuilder("{");
        for (int k = 0; k < values.length; k++) {
            text.append(k == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%.4f", values[k]));
        }

        return text.append("}").toString();
    }
}
