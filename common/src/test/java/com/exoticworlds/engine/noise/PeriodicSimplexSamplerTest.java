package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class PeriodicSimplexSamplerTest {
    private static final long SEED = 0x0153EL;
    private static final int LINE_SAMPLES = 256;
    private static final double MIN_SPREAD = 0.1;
    private static final int SIXTEENTHS = 16;

    private static final long[] WORLD_SEEDS = {0x0153EL, 0xC0FFEEL, -1234567890123456789L};

    private static final double[] SCALES = {0.0125, 0.025, 0.05, 0.09, 0.125, 0.2, 1.0};

    private static final double[] SPREAD_SCALES = {0.05, 0.2, 1.0};

    private static final WorldFold DEFAULT = fold(-16, 16, -16, 16);
    private static final WorldFold SMALL = fold(-8, 8, -8, 8);
    private static final WorldFold UNEVEN = fold(-32, 32, 0, 16);
    private static final WorldFold X_ONLY = WorldFolds.of(ShapeFixtures.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE)));

    private static final List<WorldFold> BOTH_AXES = List.of(DEFAULT, SMALL, UNEVEN);
    private static final List<WorldFold> WRAPPED_X = List.of(DEFAULT, SMALL, UNEVEN, X_ONLY);

    private static final double SEAM_EPSILON = 1.0E-9;

    private static final double CONTINUITY_TOLERANCE = 1.0E-6;

    private static final double VANILLA_SEAM_GAP_FLOOR = 0.05;

    private static final double VANILLA_KERNEL_RADIUS_SQR = 0.5;

    private static WorldFold fold(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return WorldFolds.of(
                ShapeFixtures.torus(ShapeFixtures.loopedBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax)));
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record NoiseInstance(SimplexNoise vanilla, byte[] permutations, double xo, double zo) {
        static NoiseInstance of(long worldSeed) {
            SimplexNoise vanilla = new SimplexNoise(new LegacyRandomSource(worldSeed));
            return new NoiseInstance(vanilla, vanilla.perms, vanilla.offsetX, vanilla.offsetY);
        }

        double sample(WorldFold fold, double scale, double x, double z) {
            return (float) PeriodicSimplexSampler.sample(permutations, xo, zo, fold, scale, x, z);
        }
    }

    private static double blockInDomain(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength) + sixteenth(random);
    }

    private static double lineCoord(Random random, WrapDomain domain, int step) {
        if (domain instanceof WrapDomain.Noop) {
            return -2048.0 + step * (4096.0 / LINE_SAMPLES) + sixteenth(random);
        }

        return domain.lowerBound + step * ((double) domain.domainLength / LINE_SAMPLES) + sixteenth(random);
    }

    private static double sixteenth(Random random) {
        return random.nextInt(SIXTEENTHS) / (double) SIXTEENTHS;
    }

    private static String at(WorldFold fold, long worldSeed, double scale) {
        return "in " + fold + " with seed " + worldSeed + " and scale " + scale;
    }

    @Nested
    class SkewDerivation {
        @Test
        void sharesOneDenominatorBetweenTheTwoAxes() {
            assertEquals(26, PeriodicNoiseSampler.period(DEFAULT.blockDomain(Direction.Axis.X), 0.05,
                    LapFloor.of(DEFAULT)));
            assertEquals(26, PeriodicSimplexSampler.skewDenominator(26, 26));
            assertEquals(9, PeriodicSimplexSampler.skewNumerator(26));

            assertEquals(102, PeriodicNoiseSampler.period(DEFAULT.blockDomain(Direction.Axis.X), 0.2,
                    LapFloor.of(DEFAULT)));
            assertEquals(37, PeriodicSimplexSampler.skewNumerator(102));
        }

        @Test
        void keepsTheLeftOutCornerOutsideVanillasKernel() {
            for (long denominator = 1; denominator <= 4096; denominator++) {
                double skew = (double) PeriodicSimplexSampler.skewNumerator(denominator) / denominator;
                double unskew = skew / (1.0 + 2.0 * skew);
                double leftOutCornerSqr = square(1.0 - 2.0 * unskew)
                        / (square(1.0 - unskew) + square(unskew));
                long at = denominator;
                assertTrue(leftOutCornerSqr >= VANILLA_KERNEL_RADIUS_SQR,
                        () -> "left-out corner sits at " + leftOutCornerSqr + " with denominator " + at);
            }
        }

        private static double square(double value) {
            return value * value;
        }

        @Test
        void takesTheLoopedAxisWhenTheOtherIsUnbounded() {
            long xPeriod = PeriodicNoiseSampler.period(X_ONLY.blockDomain(Direction.Axis.X), 0.05, LapFloor.of(X_ONLY));
            long zPeriod = PeriodicNoiseSampler.period(X_ONLY.blockDomain(Direction.Axis.Z), 0.05, LapFloor.of(X_ONLY));
            assertEquals(0, zPeriod);
            assertEquals(xPeriod, PeriodicSimplexSampler.skewDenominator(xPeriod, zPeriod));
        }

        @Test
        void collapsesToZeroWhenThePeriodsAreCoprime() {
            long xPeriod = PeriodicNoiseSampler.period(UNEVEN.blockDomain(Direction.Axis.X), 0.05, LapFloor.of(UNEVEN));
            long zPeriod = PeriodicNoiseSampler.period(UNEVEN.blockDomain(Direction.Axis.Z), 0.05, LapFloor.of(UNEVEN));
            assertEquals(51, xPeriod);
            assertEquals(13, zPeriod);
            assertEquals(1, PeriodicSimplexSampler.skewDenominator(xPeriod, zPeriod));
            assertEquals(0, PeriodicSimplexSampler.skewNumerator(1));
        }

        @Test
        void keepsVanillaSkewWhenNothingWraps() {
            assertEquals(0, PeriodicSimplexSampler.skewDenominator(0, 0));
            assertEquals(0, PeriodicSimplexSampler.skewNumerator(0));
        }
    }

    @Nested
    class HeldOctave {
        private static final double STARVED_SCALE = 1.0 / 1024.0;

        @Test
        void aCylinderHoldsAStarvedOctaveAroundTheRingAndVariesAlongTheOpenAxis() {
            Random random = new Random(SEED);
            WrapDomain ring = X_ONLY.blockDomain(Direction.Axis.X);
            WrapDomain open = X_ONLY.blockDomain(Direction.Axis.Z);
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                double min = Double.MAX_VALUE;
                double max = -Double.MAX_VALUE;
                for (int i = 0; i < LINE_SAMPLES; i++) {
                    double z = lineCoord(random, open, i);
                    double x = blockInDomain(random, ring);
                    double elsewhere = blockInDomain(random, ring);

                    double base = noise.sample(X_ONLY, STARVED_SCALE, x, z);
                    double around = noise.sample(X_ONLY, STARVED_SCALE, elsewhere, z);
                    assertEquals(base, around,
                            () -> "a starved octave varies around the ring between x=" + x + " and x=" + elsewhere
                                    + " at z=" + z + " with seed " + worldSeed);
                    assertTrue(Double.isFinite(base), () -> "sample at z=" + z + " is not finite");
                    min = Math.min(min, base);
                    max = Math.max(max, base);
                }

                double spread = max - min;
                assertTrue(spread >= MIN_SPREAD,
                        () -> "a held octave is flat along the open axis, spread " + spread + " with seed " + worldSeed);
            }
        }
    }

    @Nested
    class XAxis {
        @Test
        void agreesOneWorldWidthApartAlongTheWholeSeamLine() {
            Random random = new Random(SEED);
            for (WorldFold fold : WRAPPED_X) {
                double period = fold.blockDomain(Direction.Axis.X).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = blockInDomain(random, fold.blockDomain(Direction.Axis.X));
                            double z = lineCoord(random, fold.blockDomain(Direction.Axis.Z), i);

                            double base = noise.sample(fold, scale, x, z);
                            double lap = noise.sample(fold, scale, x + period, z);
                            assertEquals(base, lap,
                                    () -> "sample(" + x + ", " + z + ") vs one X lap "
                                            + at(fold, worldSeed, scale));
                        }
                    }
                }
            }
        }
    }

    @Nested
    class ZAxis {
        @Test
        void agreesOneWorldWidthApartAlongTheWholeSeamLine() {
            Random random = new Random(SEED);
            for (WorldFold fold : BOTH_AXES) {
                double period = fold.blockDomain(Direction.Axis.Z).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = lineCoord(random, fold.blockDomain(Direction.Axis.X), i);
                            double z = blockInDomain(random, fold.blockDomain(Direction.Axis.Z));

                            double base = noise.sample(fold, scale, x, z);
                            double lap = noise.sample(fold, scale, x, z + period);
                            assertEquals(base, lap,
                                    () -> "sample(" + x + ", " + z + ") vs one Z lap "
                                            + at(fold, worldSeed, scale));
                        }
                    }
                }
            }
        }
    }

    @Nested
    class Corner {
        @Test
        void agreesWhenBothAxesWrapAtOnce() {
            Random random = new Random(SEED);
            for (WorldFold fold : BOTH_AXES) {
                double xPeriod = fold.blockDomain(Direction.Axis.X).domainLength;
                double zPeriod = fold.blockDomain(Direction.Axis.Z).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = blockInDomain(random, fold.blockDomain(Direction.Axis.X));
                            double z = blockInDomain(random, fold.blockDomain(Direction.Axis.Z));

                            double base = noise.sample(fold, scale, x, z);
                            double corner = noise.sample(fold, scale, x + xPeriod, z + zPeriod);
                            assertEquals(base, corner,
                                    () -> "sample(" + x + ", " + z + ") vs the corner lap "
                                            + at(fold, worldSeed, scale));
                        }
                    }
                }
            }
        }
    }

    @Nested
    class SeamContinuity {
        @Test
        void closesAcrossTheXSeamAlongTheWholeLine() {
            Random random = new Random(SEED);
            for (WorldFold fold : WRAPPED_X) {
                WrapDomain xDomain = fold.blockDomain(Direction.Axis.X);
                double before = xDomain.upperBound - SEAM_EPSILON;
                double after = xDomain.lowerBound;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double z = lineCoord(random, fold.blockDomain(Direction.Axis.Z), i);
                            double gap = Math.abs(noise.sample(fold, scale, before, z)
                                    - noise.sample(fold, scale, after, z));
                            assertTrue(gap <= CONTINUITY_TOLERANCE,
                                    () -> "X seam gap at z " + z + " is " + gap + " "
                                            + at(fold, worldSeed, scale));
                        }
                    }
                }
            }
        }

        @Test
        void closesAcrossTheZSeamAlongTheWholeLine() {
            Random random = new Random(SEED);
            for (WorldFold fold : BOTH_AXES) {
                WrapDomain zDomain = fold.blockDomain(Direction.Axis.Z);
                double before = zDomain.upperBound - SEAM_EPSILON;
                double after = zDomain.lowerBound;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = lineCoord(random, fold.blockDomain(Direction.Axis.X), i);
                            double gap = Math.abs(noise.sample(fold, scale, x, before)
                                    - noise.sample(fold, scale, x, after));
                            assertTrue(gap <= CONTINUITY_TOLERANCE,
                                    () -> "Z seam gap at x " + x + " is " + gap + " "
                                            + at(fold, worldSeed, scale));
                        }
                    }
                }
            }
        }

        @Test
        void vanillaTearsTheSameSeamItIsAskedToClose() {
            Random random = new Random(SEED);
            WrapDomain xDomain = DEFAULT.blockDomain(Direction.Axis.X);
            double before = xDomain.upperBound - SEAM_EPSILON;
            double after = xDomain.lowerBound;
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (double scale : SCALES) {
                    double widest = 0.0;
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        double z = lineCoord(random, DEFAULT.blockDomain(Direction.Axis.Z), i) * scale;
                        widest = Math.max(widest, Math.abs(noise.vanilla().get(before * scale, z)
                                - noise.vanilla().get(after * scale, z)));
                    }

                    double measured = widest;
                    assertTrue(measured > VANILLA_SEAM_GAP_FLOOR,
                            () -> "vanilla's widest X seam gap is only " + measured + " "
                                    + at(DEFAULT, worldSeed, scale));
                }
            }
        }
    }

    @Nested
    class SkewedLattice {
        private static final List<DeckGroupFold> SKEWED = List.of(
                new DeckGroupFold(FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-16, 16, -16, 16), 5),
                        GenerationOptions.DEFAULT),
                new DeckGroupFold(FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-8, 8, -8, 8), -3),
                        GenerationOptions.DEFAULT),
                new DeckGroupFold(FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-32, 32, 0, 16), 13),
                        GenerationOptions.DEFAULT));

        @Test
        void agreesAtEveryCopyUnderBothDeckGenerators() {
            Random random = new Random(SEED);
            for (DeckGroupFold fold : SKEWED) {
                TranslationLattice lattice = fold.blockLattice();
                double width = lattice.x().domainLength;
                double height = lattice.z().domainLength;
                double skew = lattice.skew();
                double[][] copies = {{width, 0.0}, {skew, height}, {skew - width, height}, {-2.0 * skew, -2.0 * height}};
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = blockInDomain(random, lattice.x());
                            double z = blockInDomain(random, lattice.z());
                            double base = noise.sample(fold, scale, x, z);
                            for (double[] copy : copies) {
                                double moved = noise.sample(fold, scale, x + copy[0], z + copy[1]);
                                assertEquals(base, moved, () -> "sample(" + x + ", " + z + ") vs its copy "
                                        + copy[0] + ", " + copy[1] + " " + at(fold, worldSeed, scale));
                            }
                        }
                    }
                }
            }
        }

        @Test
        void closesAcrossBothSeamsAlongTheWholeLine() {
            Random random = new Random(SEED);
            for (DeckGroupFold fold : SKEWED) {
                TranslationLattice lattice = fold.blockLattice();
                WrapDomain xDomain = lattice.x();
                WrapDomain zDomain = lattice.z();
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double z = lineCoord(random, zDomain, i);
                            double xGap = Math.abs(noise.sample(fold, scale, xDomain.upperBound - SEAM_EPSILON, z)
                                    - noise.sample(fold, scale, xDomain.lowerBound, z));
                            assertTrue(xGap <= CONTINUITY_TOLERANCE, () -> "X seam gap at z " + z + " is " + xGap
                                    + " " + at(fold, worldSeed, scale));

                            double x = lineCoord(random, xDomain, i);
                            double zGap = Math.abs(noise.sample(fold, scale, x, zDomain.upperBound - SEAM_EPSILON)
                                    - noise.sample(fold, scale, x - lattice.skew(), zDomain.lowerBound));
                            assertTrue(zGap <= CONTINUITY_TOLERANCE, () -> "Z seam gap at x " + x + " is " + zGap
                                    + " " + at(fold, worldSeed, scale));
                        }
                    }
                }
            }
        }

        @Test
        void outputVariesAroundTheWorld() {
            Random random = new Random(SEED);
            for (DeckGroupFold fold : SKEWED) {
                TranslationLattice lattice = fold.blockLattice();
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SPREAD_SCALES) {
                        double min = Double.MAX_VALUE;
                        double max = -Double.MAX_VALUE;
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double value = noise.sample(fold, scale, lineCoord(random, lattice.x(), i),
                                    lineCoord(random, lattice.z(), i));
                            min = Math.min(min, value);
                            max = Math.max(max, value);
                        }

                        double spread = max - min;
                        assertTrue(spread >= MIN_SPREAD, () -> "spread around the world is " + spread + " "
                                + at(fold, worldSeed, scale));
                    }
                }
            }
        }
    }

    @Nested
    class VanillaParity {
        @Test
        void reproducesVanillaBitForBitOnAnUnboundedFold() {
            Random random = new Random(SEED);
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (double scale : SCALES) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        double x = lineCoord(random, WorldFolds.NOOP.blockDomain(Direction.Axis.X), i);
                        double z = lineCoord(random, WorldFolds.NOOP.blockDomain(Direction.Axis.Z), i);

                        double periodic = noise.sample(WorldFolds.NOOP, scale, x, z);
                        double vanilla = noise.vanilla().get(x * scale, z * scale);
                        assertEquals(vanilla, periodic,
                                () -> "sample(" + x + ", " + z + ") vs vanilla "
                                        + at(WorldFolds.NOOP, worldSeed, scale));
                    }
                }
            }
        }
    }

    @Nested
    class Degeneracy {
        @Test
        void outputVariesAlongTheSeamLineAndAroundTheWorld() {
            Random random = new Random(SEED);
            for (WorldFold fold : WRAPPED_X) {
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SPREAD_SCALES) {
                        double alongSeam = spread(random, noise, fold, scale, true);
                        assertTrue(alongSeam >= MIN_SPREAD,
                                () -> "spread along the seam line is " + alongSeam + " "
                                        + at(fold, worldSeed, scale));

                        double aroundWorld = spread(random, noise, fold, scale, false);
                        assertTrue(aroundWorld >= MIN_SPREAD,
                                () -> "spread around the world is " + aroundWorld + " "
                                        + at(fold, worldSeed, scale));
                    }
                }
            }
        }

        private double spread(Random random, NoiseInstance noise, WorldFold fold, double scale,
                boolean alongSeam) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < LINE_SAMPLES; i++) {
                WrapDomain xDomain = fold.blockDomain(Direction.Axis.X);
                double x = alongSeam ? xDomain.lowerBound : lineCoord(random, xDomain, i);
                double z = alongSeam ? lineCoord(random, fold.blockDomain(Direction.Axis.Z), i) : 5.0;
                double value = noise.sample(fold, scale, x, z);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            return max - min;
        }
    }
}
