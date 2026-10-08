package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement;

class FoldedPlacementCountsTest {
    private static final long SEED = 0x0153EL;
    private static final int LINE_SAMPLES = 256;

    private static final double[] NOISE_FACTORS = {80.0, 400.0, 1.0};
    private static final double[] NOISE_LEVELS = {0.0, -0.8};

    private static final double BAMBOO_OFFSET = 0.3;
    private static final int BAMBOO_RATIO = 160;
    private static final int GRASS_BELOW = 5;
    private static final int GRASS_ABOVE = 10;

    private static final double[] UNFOLDABLE_FACTORS = {
            0.0,
            -0.0,
            -80.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            1.0E-300,
            1.0E300,
            Double.MIN_VALUE,
            Double.MAX_VALUE
    };

    private static final WorldFold DEFAULT = torus(-16, 16, -16, 16);
    private static final WorldFold SMALL = torus(-8, 8, -8, 8);
    private static final WorldFold UNEVEN = torus(-32, 32, 0, 16);
    private static final WorldFold X_ONLY = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE)));

    private static final List<WorldFold> BOTH_AXES = List.of(DEFAULT, SMALL, UNEVEN);
    private static final List<WorldFold> WRAPPED_X = List.of(DEFAULT, SMALL, UNEVEN, X_ONLY);

    private static WorldFold torus(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax)));
    }

    private static int blockInDomain(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    private static int lineCoord(Random random, WrapDomain domain, int step) {
        if (domain instanceof WrapDomain.Noop) {
            return -2048 + step * (4096 / LINE_SAMPLES);
        }

        return domain.lowerBound + step * (domain.domainLength / LINE_SAMPLES) + random.nextInt(2);
    }

    private static int lap(WorldFold fold, Direction.Axis axis) {
        return fold.blockDomain(axis).domainLength;
    }

    @Nested
    class NoiseBasedCount {
        @Test
        void agreesOneWorldWidthApartAlongX() {
            Random random = new Random(SEED);
            for (WorldFold fold : WRAPPED_X) {
                int period = lap(fold, Direction.Axis.X);
                for (double factor : NOISE_FACTORS) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        int x = blockInDomain(random, fold.blockDomain(Direction.Axis.X));
                        int z = lineCoord(random, fold.blockDomain(Direction.Axis.Z), i);

                        assertEquals(count(fold, x, z, factor), count(fold, x + period, z, factor),
                                () -> "count(" + x + ", " + z + ") vs one X lap in " + fold
                                        + " with noise factor " + factor);
                    }
                }
            }
        }

        @Test
        void agreesOneWorldWidthApartAlongZ() {
            Random random = new Random(SEED);
            for (WorldFold fold : BOTH_AXES) {
                int period = lap(fold, Direction.Axis.Z);
                for (double factor : NOISE_FACTORS) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        int x = lineCoord(random, fold.blockDomain(Direction.Axis.X), i);
                        int z = blockInDomain(random, fold.blockDomain(Direction.Axis.Z));

                        assertEquals(count(fold, x, z, factor), count(fold, x, z + period, factor),
                                () -> "count(" + x + ", " + z + ") vs one Z lap in " + fold
                                        + " with noise factor " + factor);
                    }
                }
            }
        }

        @Test
        void agreesWhenBothAxesWrapAtOnce() {
            Random random = new Random(SEED);
            for (WorldFold fold : BOTH_AXES) {
                int xPeriod = lap(fold, Direction.Axis.X);
                int zPeriod = lap(fold, Direction.Axis.Z);
                for (double factor : NOISE_FACTORS) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        int x = blockInDomain(random, fold.blockDomain(Direction.Axis.X));
                        int z = blockInDomain(random, fold.blockDomain(Direction.Axis.Z));

                        assertEquals(count(fold, x, z, factor), count(fold, x + xPeriod, z + zPeriod, factor),
                                () -> "count(" + x + ", " + z + ") vs the corner lap in " + fold
                                        + " with noise factor " + factor);
                    }
                }
            }
        }

        private static int count(WorldFold fold, int x, int z, double factor) {
            return FoldedPlacementCounts.noiseBased(fold, x, z, factor, BAMBOO_OFFSET, BAMBOO_RATIO);
        }
    }

    @Nested
    class NoiseThresholdCount {
        @Test
        void agreesOneWorldWidthApartAlongX() {
            Random random = new Random(SEED);
            for (WorldFold fold : WRAPPED_X) {
                int period = lap(fold, Direction.Axis.X);
                for (double level : NOISE_LEVELS) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        int x = blockInDomain(random, fold.blockDomain(Direction.Axis.X));
                        int z = lineCoord(random, fold.blockDomain(Direction.Axis.Z), i);

                        assertEquals(count(fold, x, z, level), count(fold, x + period, z, level),
                                () -> "count(" + x + ", " + z + ") vs one X lap in " + fold
                                        + " with noise level " + level);
                    }
                }
            }
        }

        @Test
        void agreesOneWorldWidthApartAlongZ() {
            Random random = new Random(SEED);
            for (WorldFold fold : BOTH_AXES) {
                int period = lap(fold, Direction.Axis.Z);
                for (double level : NOISE_LEVELS) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        int x = lineCoord(random, fold.blockDomain(Direction.Axis.X), i);
                        int z = blockInDomain(random, fold.blockDomain(Direction.Axis.Z));

                        assertEquals(count(fold, x, z, level), count(fold, x, z + period, level),
                                () -> "count(" + x + ", " + z + ") vs one Z lap in " + fold
                                        + " with noise level " + level);
                    }
                }
            }
        }

        @Test
        void agreesWhenBothAxesWrapAtOnce() {
            Random random = new Random(SEED);
            for (WorldFold fold : BOTH_AXES) {
                int xPeriod = lap(fold, Direction.Axis.X);
                int zPeriod = lap(fold, Direction.Axis.Z);
                for (double level : NOISE_LEVELS) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        int x = blockInDomain(random, fold.blockDomain(Direction.Axis.X));
                        int z = blockInDomain(random, fold.blockDomain(Direction.Axis.Z));

                        assertEquals(count(fold, x, z, level), count(fold, x + xPeriod, z + zPeriod, level),
                                () -> "count(" + x + ", " + z + ") vs the corner lap in " + fold
                                        + " with noise level " + level);
                    }
                }
            }
        }

        private static int count(WorldFold fold, int x, int z, double level) {
            return FoldedPlacementCounts.noiseThreshold(fold, x, z, level, GRASS_BELOW, GRASS_ABOVE);
        }
    }

    @Nested
    class VanillaParity {
        @Test
        void reproducesVanillaOnAnUnboundedFold() {
            Random random = new Random(SEED);
            WrapDomain unbounded = WorldFolds.NOOP.blockDomain(Direction.Axis.X);
            for (double factor : NOISE_FACTORS) {
                NoiseBasedCountPlacement vanilla = NoiseBasedCountPlacement.of(BAMBOO_RATIO, factor, BAMBOO_OFFSET);
                for (int i = 0; i < LINE_SAMPLES; i++) {
                    int x = lineCoord(random, unbounded, i);
                    int z = lineCoord(random, unbounded, i);

                    assertEquals(vanilla.count(RandomSource.create(SEED), new BlockPos(x, 0, z)),
                            FoldedPlacementCounts.noiseBased(WorldFolds.NOOP, x, z, factor, BAMBOO_OFFSET,
                                    BAMBOO_RATIO),
                            () -> "noiseBased(" + x + ", " + z + ") vs vanilla with noise factor " + factor);
                }
            }
        }

        @Test
        void reproducesVanillaThresholdOnAnUnboundedFold() {
            Random random = new Random(SEED);
            WrapDomain unbounded = WorldFolds.NOOP.blockDomain(Direction.Axis.X);
            for (double level : NOISE_LEVELS) {
                NoiseThresholdCountPlacement vanilla =
                        NoiseThresholdCountPlacement.of(level, GRASS_BELOW, GRASS_ABOVE);
                for (int i = 0; i < LINE_SAMPLES; i++) {
                    int x = lineCoord(random, unbounded, i);
                    int z = lineCoord(random, unbounded, i);

                    assertEquals(vanilla.count(RandomSource.create(SEED), new BlockPos(x, 0, z)),
                            FoldedPlacementCounts.noiseThreshold(WorldFolds.NOOP, x, z, level, GRASS_BELOW,
                                    GRASS_ABOVE),
                            () -> "noiseThreshold(" + x + ", " + z + ") vs vanilla with noise level " + level);
                }
            }
        }
    }

    @Nested
    class UnfoldableFactor {
        @Test
        void returnsForEveryFactorTheLatticeCannotExpress() {
            Random random = new Random(SEED);
            for (WorldFold fold : WRAPPED_X) {
                for (double factor : UNFOLDABLE_FACTORS) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        int x = blockInDomain(random, fold.blockDomain(Direction.Axis.X));
                        int z = lineCoord(random, fold.blockDomain(Direction.Axis.Z), i);

                        assertDoesNotThrow(
                                () -> FoldedPlacementCounts.noiseBased(fold, x, z, factor, BAMBOO_OFFSET,
                                        BAMBOO_RATIO),
                                () -> "noiseBased(" + x + ", " + z + ") in " + fold + " with noise factor " + factor);
                    }
                }
            }
        }
    }
}
