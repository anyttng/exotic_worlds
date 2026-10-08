package com.exoticworlds.engine.noise;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;
import static com.exoticworlds.core.WorldFoldFixture.ODD_BOUNDS;
import static com.exoticworlds.core.WorldFoldFixture.SQUARE;
import static com.exoticworlds.core.WorldFoldFixture.UNEVEN_BOUNDS;
import static com.exoticworlds.core.WorldFoldFixture.X_ONLY_BOUNDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SmearedPerlinNoise;

class PeriodicNoiseSamplerTest {
    private static final long SEED = 0x0153EL;
    private static final int LINE_SAMPLES = 256;
    private static final double MIN_SPREAD = 0.1;
    private static final int SIXTEENTHS = 16;

    private static final long[] WORLD_SEEDS = {0x0153EL, 0xC0FFEEL, -1234567890123456789L};

    private static final double[] SCALES = {0.25, 1.0, 100.0, 1.17};

    private static final double[] PARITY_SCALES = {0.25, 1.0, 100.0};

    private static final double NO_FUDGE = 0.0;

    private static final double[] FUDGE_SCALES = {NO_FUDGE, 1.0};

    private static final NoiseFrame FRAME = new NoiseFrame(SlotAxes.DEFAULT, NoiseConstants.UNDIVIDED,
            NoiseConstants.UNDIVIDED, GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE);

    private static final WorldFold EVEN = torus(SQUARE);
    private static final WorldFold ODD = torus(ODD_BOUNDS);
    private static final WorldFold UNEVEN = torus(UNEVEN_BOUNDS);
    private static final WorldFold X_ONLY = WorldFolds.of(FlatShape.cylinder(X_ONLY_BOUNDS));

    private static final List<WorldFold> BOTH_AXES = List.of(EVEN, ODD, UNEVEN);
    private static final List<WorldFold> WRAPPED_X = List.of(EVEN, ODD, UNEVEN, X_ONLY);

    private static WorldFold torus(WorldLoopBounds bounds) {
        return WorldFolds.of(FlatShape.torus(bounds));
    }

    private record NoiseInstance(long worldSeed, PerlinNoise vanilla, byte[] permutations,
            double xo, double yo, double zo) {
        static NoiseInstance of(long worldSeed) {
            PerlinNoise vanilla = new PerlinNoise(new LegacyRandomSource(worldSeed));
            return new NoiseInstance(worldSeed, vanilla, vanilla.perms, vanilla.offsetX, vanilla.offsetY,
                    vanilla.offsetZ);
        }

        double sample(WorldFold transformer, double scale, double x, double y, double z, double fudgeYScale) {
            return fudgeYScale == NO_FUDGE
                    ? PeriodicNoiseSampler.sample(permutations, xo, yo, zo, transformer, FRAME, scale, x, y, z)
                    : PeriodicNoiseSampler.sampleSmeared(permutations, xo, yo, zo, transformer, FRAME, scale,
                            x, y, z, y, fudgeYScale);
        }

        @SuppressWarnings("deprecation")
        double vanilla(double scale, double x, double y, double z, double fudgeYScale) {
            return fudgeYScale == NO_FUDGE
                    ? this.vanilla.get(x * scale, y, z * scale)
                    : new SmearedPerlinNoise(new LegacyRandomSource(this.worldSeed), fudgeYScale)
                            .get(x * scale, y, z * scale);
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

    private static double sampleY(Random random) {
        return random.nextInt(384) - 64 + random.nextDouble();
    }

    private static String at(WorldFold transformer, long worldSeed, double scale) {
        return "in " + transformer + " with seed " + worldSeed + " and scale " + scale;
    }

    @Nested
    class PeriodDerivation {
        @Test
        void roundsClampsAndPassesUnboundedThrough() {
            WrapDomain evenX = EVEN.blockDomain(Direction.Axis.X);
            LapFloor torus = LapFloor.of(EVEN);
            assertEquals(LapFloor.TWO_CELLS, torus);
            assertEquals(256, PeriodicNoiseSampler.period(evenX, 0.25, torus));
            assertEquals(1024, PeriodicNoiseSampler.period(evenX, 1.0, torus));
            assertEquals(102400, PeriodicNoiseSampler.period(evenX, 100.0, torus));
            assertEquals(94, PeriodicNoiseSampler.period(ODD.blockDomain(Direction.Axis.X), 1.17, LapFloor.of(ODD)));
            assertEquals(2, PeriodicNoiseSampler.period(evenX, 2.0 / 1024.0, torus));
            assertEquals(2, PeriodicNoiseSampler.period(evenX, 1.0 / 2048.0, torus));
            assertEquals(0, PeriodicNoiseSampler.period(X_ONLY.blockDomain(Direction.Axis.Z), 1.0, LapFloor.of(X_ONLY)));
        }

        @Test
        void holdsAStarvedOctaveOnACylinderAndFloorsItOnATorus() {
            LapFloor cylinder = LapFloor.of(X_ONLY);
            WrapDomain ring = X_ONLY.blockDomain(Direction.Axis.X);
            assertEquals(LapFloor.HELD, cylinder);
            assertEquals(PeriodicNoiseSampler.HELD_PERIOD, PeriodicNoiseSampler.period(ring, 1.0 / 2048.0, cylinder));
            assertEquals(2, PeriodicNoiseSampler.period(ring, 2.0 / 1024.0, cylinder));
            assertEquals(2, PeriodicNoiseSampler.period(EVEN.blockDomain(Direction.Axis.X), 1.0 / 2048.0,
                    LapFloor.of(EVEN)));
        }
    }

    @Nested
    class XAxis {
        @Test
        void agreesOneWorldWidthApartAlongTheWholeSeamLine() {
            Random random = new Random(SEED);
            for (WorldFold transformer : WRAPPED_X) {
                double period = transformer.blockDomain(Direction.Axis.X).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (double fudge : FUDGE_SCALES) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = blockInDomain(random, transformer.blockDomain(Direction.Axis.X));
                                double y = sampleY(random);
                                double z = lineCoord(random, transformer.blockDomain(Direction.Axis.Z), i);

                                double base = noise.sample(transformer, scale, x, y, z, fudge);
                                double lap = noise.sample(transformer, scale, x + period, y, z, fudge);
                                assertEquals(base, lap,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs one X lap "
                                                + at(transformer, worldSeed, scale));
                            }
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
            for (WorldFold transformer : BOTH_AXES) {
                double period = transformer.blockDomain(Direction.Axis.Z).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (double fudge : FUDGE_SCALES) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = lineCoord(random, transformer.blockDomain(Direction.Axis.X), i);
                                double y = sampleY(random);
                                double z = blockInDomain(random, transformer.blockDomain(Direction.Axis.Z));

                                double base = noise.sample(transformer, scale, x, y, z, fudge);
                                double lap = noise.sample(transformer, scale, x, y, z + period, fudge);
                                assertEquals(base, lap,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs one Z lap "
                                                + at(transformer, worldSeed, scale));
                            }
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
            for (WorldFold transformer : BOTH_AXES) {
                double xPeriod = transformer.blockDomain(Direction.Axis.X).domainLength;
                double zPeriod = transformer.blockDomain(Direction.Axis.Z).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (double fudge : FUDGE_SCALES) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = blockInDomain(random, transformer.blockDomain(Direction.Axis.X));
                                double y = sampleY(random);
                                double z = blockInDomain(random, transformer.blockDomain(Direction.Axis.Z));

                                double base = noise.sample(transformer, scale, x, y, z, fudge);
                                double corner = noise.sample(transformer, scale,
                                        x + xPeriod, y, z + zPeriod, fudge);
                                assertEquals(base, corner,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs the corner lap "
                                                + at(transformer, worldSeed, scale));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    class VanillaParity {
        @Test
        void reproducesVanillaBitForBitWhenThePeriodIsAMultipleOf256() {
            Random random = new Random(SEED);
            for (WorldFold transformer : List.of(EVEN, X_ONLY)) {
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : PARITY_SCALES) {
                        for (double fudge : FUDGE_SCALES) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = blockInDomain(random, transformer.blockDomain(Direction.Axis.X));
                                double y = sampleY(random);
                                double z = lineCoord(random, transformer.blockDomain(Direction.Axis.Z), i);

                                double periodic = noise.sample(transformer, scale, x, y, z, fudge);
                                double vanilla = noise.vanilla(scale, x, y, z, fudge);
                                assertEquals(vanilla, periodic,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs vanilla "
                                                + at(transformer, worldSeed, scale));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    class LowestPeriod {
        private static final double TINY_SCALE = 1.0 / 2048.0;

        @Test
        void closesTheLapAtPeriodOne() {
            Random random = new Random(SEED);
            double period = EVEN.blockDomain(Direction.Axis.X).domainLength;
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (int i = 0; i < LINE_SAMPLES; i++) {
                    double x = blockInDomain(random, EVEN.blockDomain(Direction.Axis.X));
                    double y = sampleY(random);
                    double z = blockInDomain(random, EVEN.blockDomain(Direction.Axis.Z));

                    double base = noise.sample(EVEN, TINY_SCALE, x, y, z, NO_FUDGE);
                    double lap = noise.sample(EVEN, TINY_SCALE, x + period, y, z, NO_FUDGE);
                    assertTrue(Double.isFinite(base),
                            () -> "sample(" + x + ", " + y + ", " + z + ") is not finite at period 1");
                    assertEquals(base, lap,
                            () -> "sample(" + x + ", " + y + ", " + z + ") vs one X lap at period 1");
                }
            }
        }
    }

    @Nested
    class HeldOctave {
        private static final double STARVED_SCALE = 1.0 / 2048.0;

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
                    double y = sampleY(random);
                    double z = lineCoord(random, open, i);
                    double x = blockInDomain(random, ring);
                    double elsewhere = blockInDomain(random, ring);

                    double base = noise.sample(X_ONLY, STARVED_SCALE, x, y, z, NO_FUDGE);
                    double around = noise.sample(X_ONLY, STARVED_SCALE, elsewhere, y, z, NO_FUDGE);
                    assertEquals(base, around,
                            () -> "a starved octave varies around the ring between x=" + x + " and x=" + elsewhere
                                    + " at z=" + z + " with seed " + worldSeed);
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
    class Degeneracy {
        @Test
        void outputVariesAlongTheSeamLineAndAroundTheWorld() {
            Random random = new Random(SEED);
            for (WorldFold transformer : WRAPPED_X) {
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        double alongSeam = spread(random, noise, transformer, scale, true);
                        assertTrue(alongSeam >= MIN_SPREAD,
                                () -> "spread along the seam line is " + alongSeam + " "
                                        + at(transformer, worldSeed, scale));

                        double aroundWorld = spread(random, noise, transformer, scale, false);
                        assertTrue(aroundWorld >= MIN_SPREAD,
                                () -> "spread around the world is " + aroundWorld + " "
                                        + at(transformer, worldSeed, scale));
                    }
                }
            }
        }

        private double spread(Random random, NoiseInstance noise, WorldFold transformer, double scale,
                boolean alongSeam) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            double y = sampleY(random);
            for (int i = 0; i < LINE_SAMPLES; i++) {
                WrapDomain xDomain = transformer.blockDomain(Direction.Axis.X);
                double x = alongSeam ? xDomain.lowerBound : lineCoord(random, xDomain, i);
                double z = alongSeam ? lineCoord(random, transformer.blockDomain(Direction.Axis.Z), i) : 5.0;
                double value = noise.sample(transformer, scale, x, y, z, NO_FUDGE);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return max - min;
        }
    }
}
