package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import org.betterx.wover.math.api.noise.OpenSimplexNoise;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

class OpenSimplexStandInTest {
    private static final long SEED = 0x900L;

    private static final int FIELDS = 16;

    private static final int LINES = 32;

    private static final int LINE_SAMPLES = 20000;

    private static final double STEP = 0.01;

    private static final double LINE_SPACING = 3.7;

    private static final double ORIGIN_SPREAD = 1.0E4;

    private static final double TOLERANCE = 0.03;

    private static final double PERIOD_TOLERANCE = 1.0E-9;

    private static final double ROW_MIDDLE = 0.5;

    private static final double FLIP_STEP = 0.05;

    private static final int WARP_OCTAVES = 5;

    private static final double QUART_STEP = 0.125;

    private static final int QUART_SAMPLES = 1600;

    private static final int SUM = 0;

    private static final int AXES = 3;

    private static final int FIELDS_3D = 64;

    private interface Field {
        double at(double x, double z);
    }

    private record Warp(OpenSimplexNoise[] own, OpenSimplexStandIn[] standIn) {
        static Warp of(Random random) {
            return new Warp(new OpenSimplexNoise[] {new OpenSimplexNoise(random.nextLong()),
                    new OpenSimplexNoise(random.nextLong())},
                    new OpenSimplexStandIn[] {new OpenSimplexStandIn(random.nextLong()),
                            new OpenSimplexStandIn(random.nextLong())});
        }

        Field ownOctave(int octave) {
            return (x, z) -> this.own[(octave - 1) & 1].eval(x * octave, z * octave) / octave;
        }

        Field standInOctave(int octave) {
            return (x, z) -> this.standIn[(octave - 1) & 1].eval(x * octave, z * octave,
                    OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED) / octave;
        }

        Field own(int octave) {
            return octave == SUM ? sum(this::ownOctave) : ownOctave(octave);
        }

        Field standIn(int octave) {
            return octave == SUM ? sum(this::standInOctave) : standInOctave(octave);
        }

        private static Field sum(IntFunction<Field> octaves) {
            return (x, z) -> {
                double result = 0.0;
                for (int octave = 1; octave <= WARP_OCTAVES; octave++) {
                    result += octaves.apply(octave).at(x, z);
                }

                return result;
            };
        }
    }

    private static final class Increments {
        private double squares;

        private double incrementSquares;

        private long samples;

        private long crossings;

        private double length;

        void line(Field field, double originX, double z) {
            double previous = field.at(originX, z);
            for (int sample = 1; sample < QUART_SAMPLES; sample++) {
                double value = field.at(originX + sample * QUART_STEP, z);
                this.squares += value * value;
                this.incrementSquares += (value - previous) * (value - previous);
                this.samples++;
                if ((value > 0.0) != (previous > 0.0)) {
                    this.crossings++;
                }

                previous = value;
            }

            this.length += QUART_SAMPLES * QUART_STEP;
        }

        double deviation() {
            return Math.sqrt(this.squares / this.samples);
        }

        double quartIncrement() {
            return Math.sqrt(this.incrementSquares / this.samples);
        }

        double crossingsPerUnit() {
            return this.crossings / this.length;
        }

        String against(Increments other) {
            return String.format(Locale.ROOT, "spread %.4f / %.4f, crossings per unit %.3f / %.3f, "
                    + "quart increment %.4f / %.4f", deviation(), other.deviation(), crossingsPerUnit(),
                    other.crossingsPerUnit(), quartIncrement(), other.quartIncrement());
        }
    }

    @Test
    void theWarpSumMovesAQuartLikeWorldWeaversOnEveryOctave() {
        Random random = new Random(SEED);
        Increments[] own = new Increments[WARP_OCTAVES + 1];
        Increments[] standIn = new Increments[WARP_OCTAVES + 1];
        for (int octave = SUM; octave <= WARP_OCTAVES; octave++) {
            own[octave] = new Increments();
            standIn[octave] = new Increments();
        }

        for (int field = 0; field < FIELDS; field++) {
            Warp warp = Warp.of(random);
            double originX = random.nextDouble() * ORIGIN_SPREAD;
            double originZ = random.nextDouble() * ORIGIN_SPREAD;
            for (int line = 0; line < LINES; line++) {
                double z = originZ + line * LINE_SPACING;
                for (int octave = SUM; octave <= WARP_OCTAVES; octave++) {
                    own[octave].line(warp.own(octave), originX, z);
                    standIn[octave].line(warp.standIn(octave), originX, z);
                }
            }
        }

        StringBuilder table = new StringBuilder("stand-in / OpenSimplex");
        for (int octave = SUM; octave <= WARP_OCTAVES; octave++) {
            table.append("; ").append(octave == SUM ? "sum" : "octave " + octave).append(": ")
                    .append(standIn[octave].against(own[octave]));
        }

        String readings = table.toString();
        assertEquals(1.0, standIn[SUM].deviation() / own[SUM].deviation(), TOLERANCE, readings);
        assertEquals(1.0, standIn[SUM].quartIncrement() / own[SUM].quartIncrement(), TOLERANCE, readings);
    }

    @Test
    void theStandInSpreadsAndCrossesZeroLikeWorldWeaversOpenSimplex() {
        Random random = new Random(SEED);
        Moments openSimplex = new Moments();
        Moments standIn = new Moments();
        for (int field = 0; field < FIELDS; field++) {
            OpenSimplexNoise reference = new OpenSimplexNoise(random.nextLong());
            OpenSimplexStandIn candidate = new OpenSimplexStandIn(random.nextLong());
            double originX = random.nextDouble() * ORIGIN_SPREAD;
            double originZ = random.nextDouble() * ORIGIN_SPREAD;
            for (int line = 0; line < LINES; line++) {
                double z = originZ + line * LINE_SPACING;
                openSimplex.line(x -> reference.eval(x, z), originX);
                standIn.line(x -> candidate.eval(x, z, OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED),
                        originX);
            }
        }

        assertEquals(1.0, standIn.deviation() / openSimplex.deviation(), TOLERANCE,
                "spread: stand-in " + standIn.deviation() + " against OpenSimplex " + openSimplex.deviation());
        assertEquals(1.0, standIn.crossingsPerUnit() / openSimplex.crossingsPerUnit(), TOLERANCE,
                "zero crossings per unit: stand-in " + standIn.crossingsPerUnit() + " against OpenSimplex "
                        + openSimplex.crossingsPerUnit());
    }

    @Test
    void theThreeDimensionalStandInSpreadsAndCrossesZeroLikeOpenSimplexAlongEveryAxis() {
        StringBuilder readings = new StringBuilder("stand-in / OpenSimplex in three dimensions");
        double[][] ratios = new double[AXES][];
        for (int axis = 0; axis < AXES; axis++) {
            Random random = new Random(SEED + axis);
            Moments openSimplex = new Moments();
            Moments standIn = new Moments();
            for (int field = 0; field < FIELDS_3D; field++) {
                OpenSimplexNoise reference = new OpenSimplexNoise(random.nextLong());
                OpenSimplexStandIn candidate = new OpenSimplexStandIn(random.nextLong());
                double[] origin = {random.nextDouble() * ORIGIN_SPREAD, random.nextDouble() * ORIGIN_SPREAD,
                        random.nextDouble() * ORIGIN_SPREAD};
                for (int line = 0; line < LINES; line++) {
                    double[] start = origin.clone();
                    start[(axis + 1) % AXES] += line * LINE_SPACING;
                    int along = axis;
                    openSimplex.line(t -> reference.eval(at(start, along, t, 0), at(start, along, t, 1),
                            at(start, along, t, 2)), 0.0);
                    standIn.line(t -> candidate.eval(at(start, along, t, 0), at(start, along, t, 1),
                            at(start, along, t, 2), OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED), 0.0);
                }
            }

            ratios[axis] = new double[] {standIn.deviation() / openSimplex.deviation(),
                    standIn.crossingsPerUnit() / openSimplex.crossingsPerUnit()};
            readings.append(String.format(Locale.ROOT, "; axis %d: spread %.4f / %.4f, crossings per unit %.4f / %.4f",
                    axis, standIn.deviation(), openSimplex.deviation(), standIn.crossingsPerUnit(),
                    openSimplex.crossingsPerUnit()));
        }

        for (double[] ratio : ratios) {
            assertEquals(1.0, ratio[0], TOLERANCE, readings.toString());
            assertEquals(1.0, ratio[1], TOLERANCE, readings.toString());
        }
    }

    private static double at(double[] start, int along, double t, int axis) {
        return axis == along ? start[axis] + t : start[axis];
    }

    @Test
    void aClosedPeriodRepeatsInThreeDimensions() {
        OpenSimplexStandIn noise = new OpenSimplexStandIn(SEED);
        double[] periods = {1.0, 7.0, 21.0, 110.0};
        for (double xPeriod : periods) {
            for (double zPeriod : periods) {
                for (double x = -3.0; x < 3.0; x += 0.37) {
                    for (double z = -3.0; z < 3.0; z += 0.41) {
                        double y = x * z;
                        double here = noise.eval(x, y, z, xPeriod, zPeriod);
                        assertEquals(here, noise.eval(x + xPeriod, y, z, xPeriod, zPeriod), PERIOD_TOLERANCE,
                                "x period " + xPeriod + " at " + x + ", " + y + ", " + z);
                        assertEquals(here, noise.eval(x, y, z + zPeriod, xPeriod, zPeriod), PERIOD_TOLERANCE,
                                "z period " + zPeriod + " at " + x + ", " + y + ", " + z);
                    }
                }
            }
        }
    }

    @Test
    void aClosedPeriodNeverWidensTheThreeDimensionalFeature() {
        double[] periods = {0.5, 1.0, 2.5, 8.0, 110.0};
        for (double period : periods) {
            long cells = OpenSimplexStandIn.cells(period, OpenSimplexStandIn.RATE_3D);
            assertTrue(cells >= period * OpenSimplexStandIn.RATE_3D - PERIOD_TOLERANCE,
                    cells + " lattice cells over a period of " + period);
        }
    }

    @Test
    void theZeroSetCrossesALatticeRowNoMoreOftenThanALineBetweenRows() {
        Random random = new Random(SEED);
        long rowFlips = 0;
        long betweenFlips = 0;
        long pairs = 0;
        for (int field = 0; field < FIELDS; field++) {
            long seed = random.nextLong();
            OpenSimplexStandIn noise = new OpenSimplexStandIn(seed);
            double zo = new ImprovedNoise(new WorldgenRandom(new LegacyRandomSource(seed))).zo;
            for (int row = 0; row < LINES; row++) {
                double onRow = (row - zo) / OpenSimplexStandIn.RATE;
                double betweenRows = (row + ROW_MIDDLE - zo) / OpenSimplexStandIn.RATE;
                for (int sample = 0; sample < LINE_SAMPLES; sample++) {
                    double x = sample * FLIP_STEP;
                    pairs++;
                    rowFlips += flips(noise, x, onRow) ? 1 : 0;
                    betweenFlips += flips(noise, x, betweenRows) ? 1 : 0;
                }
            }
        }

        double ratio = (double) rowFlips / betweenFlips;
        assertTrue(ratio <= 1.0, "the zero set crosses a lattice row on "
                + rowFlips + " of " + pairs + " steps and a line between rows on " + betweenFlips + ": ratio " + ratio);
    }

    private static boolean flips(OpenSimplexStandIn noise, double x, double z) {
        double before = noise.eval(x, z - FLIP_STEP, OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED);
        double after = noise.eval(x, z + FLIP_STEP, OpenSimplexStandIn.UNBOUNDED, OpenSimplexStandIn.UNBOUNDED);
        return (before > 0.0) != (after > 0.0);
    }

    @Test
    void aClosedPeriodRepeatsOnBothAxes() {
        OpenSimplexStandIn noise = new OpenSimplexStandIn(SEED);
        double[] periods = {1.0, 7.0, 8.0, 21.0, 110.0};
        for (double xPeriod : periods) {
            for (double zPeriod : periods) {
                for (double x = -3.0; x < 3.0; x += 0.37) {
                    for (double z = -3.0; z < 3.0; z += 0.41) {
                        double here = noise.eval(x, z, xPeriod, zPeriod);
                        assertEquals(here, noise.eval(x + xPeriod, z, xPeriod, zPeriod), PERIOD_TOLERANCE,
                                "x period " + xPeriod + " at " + x + ", " + z);
                        assertEquals(here, noise.eval(x, z + zPeriod, xPeriod, zPeriod), PERIOD_TOLERANCE,
                                "z period " + zPeriod + " at " + x + ", " + z);
                    }
                }
            }
        }
    }

    @Test
    void aClosedPeriodNeverWidensTheFeature() {
        double[] periods = {0.5, 1.0, 2.5, 8.0, 110.0};
        for (double period : periods) {
            long cells = OpenSimplexStandIn.cells(period);
            assertTrue(cells >= period * OpenSimplexStandIn.RATE - PERIOD_TOLERANCE,
                    cells + " lattice cells over a period of " + period);
        }
    }

    private static final class Moments {
        private double squares;

        private long samples;

        private long crossings;

        private double length;

        void line(DoubleUnaryOperator field, double originX) {
            double previous = field.applyAsDouble(originX);
            for (int sample = 1; sample < LINE_SAMPLES; sample++) {
                double value = field.applyAsDouble(originX + sample * STEP);
                this.squares += value * value;
                this.samples++;
                if ((value > 0.0) != (previous > 0.0)) {
                    this.crossings++;
                }

                previous = value;
            }

            this.length += LINE_SAMPLES * STEP;
        }

        double deviation() {
            return Math.sqrt(this.squares / this.samples);
        }

        double crossingsPerUnit() {
            return this.crossings / this.length;
        }
    }
}
