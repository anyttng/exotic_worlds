package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class PeriodicEndIslandsTest {
    private static final long SEED = 0x0153EL;
    private static final int SAMPLES = 512;
    private static final int ISLAND_NOISE_SKIP = 17292;

    private static final int SWEEP_REACH_BLOCKS = 13 * 16;
    private static final int MAIN_ISLAND_REACH_BLOCKS = 96;
    private static final int UNBOUNDED_SPAN = 4096;
    private static final float NO_ISLAND_HEIGHT = -100.0F;

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-128, 128, -96, 96)));
    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-128, 128), AxisBounds.Unbounded.INSTANCE)));

    private static final List<WorldFold> WRAPPED_X = List.of(TORUS, CYLINDER);
    private static final List<WorldFold> WRAPPED_Z = List.of(TORUS);

    private static final SimplexNoise ISLAND_NOISE = islandNoise();

    private static SimplexNoise islandNoise() {
        RandomSource random = new LegacyRandomSource(SEED);
        random.consumeCount(ISLAND_NOISE_SKIP);
        return new SimplexNoise(random);
    }

    @Nested
    class InsideTheDomain {
        @Test
        void aWrappedFoldReadsLikeNoFoldWhereTheSweepStaysClearOfTheSeam() {
            Random random = new Random(SEED);
            for (WorldFold fold : WRAPPED_X) {
                int hits = 0;
                for (int i = 0; i < SAMPLES; i++) {
                    int x = clearOfSeam(random, fold.blockDomain(Direction.Axis.X));
                    int z = clearOfSeam(random, fold.blockDomain(Direction.Axis.Z));
                    float folded = PeriodicEndIslands.heightValue(ISLAND_NOISE, fold, x, z);
                    assertEquals(PeriodicEndIslands.heightValue(ISLAND_NOISE, WorldFolds.NOOP, x, z), folded,
                            () -> "at (" + x + ", " + z + ") in " + fold);
                    hits += folded > NO_ISLAND_HEIGHT ? 1 : 0;
                }

                assertStoodOnIslands(hits, "inside " + fold);
            }
        }
    }

    @Nested
    class XAxis {
        @Test
        void theMainIslandRepeatsOneWorldWidthAway() {
            mainIslandRepeats(WRAPPED_X, Direction.Axis.X);
        }

        @Test
        void outerIslandsRepeatOneWorldWidthAwayAcrossTheSeam() {
            outerIslandsRepeat(WRAPPED_X, Direction.Axis.X);
        }
    }

    @Nested
    class ZAxis {
        @Test
        void theMainIslandRepeatsOneWorldWidthAway() {
            mainIslandRepeats(WRAPPED_Z, Direction.Axis.Z);
        }

        @Test
        void outerIslandsRepeatOneWorldWidthAwayAcrossTheSeam() {
            outerIslandsRepeat(WRAPPED_Z, Direction.Axis.Z);
        }
    }

    @Nested
    class VanillaParity {
        @Test
        void noFoldReproducesVanillaInTheNonNegativeQuadrant() {
            DensityFunction vanilla = DensityFunctions.endIslands(SEED);
            Random random = new Random(SEED);
            int hits = 0;
            for (int i = 0; i < SAMPLES; i++) {
                int x = random.nextInt(UNBOUNDED_SPAN);
                int z = random.nextInt(UNBOUNDED_SPAN);
                float height = PeriodicEndIslands.heightValue(ISLAND_NOISE, WorldFolds.NOOP, x, z);
                assertEquals(vanilla.compute(new DensityFunction.SinglePointContext(x, 0, z)),
                        PeriodicEndIslands.density(height), () -> "at (" + x + ", " + z + ")");
                hits += height > NO_ISLAND_HEIGHT ? 1 : 0;
            }

            assertStoodOnIslands(hits, "in the non-negative quadrant");
        }
    }

    private static void mainIslandRepeats(List<WorldFold> folds, Direction.Axis axis) {
        Random random = new Random(SEED);
        for (WorldFold fold : folds) {
            int width = fold.blockDomain(axis).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int along = random.nextInt(2 * MAIN_ISLAND_REACH_BLOCKS) - MAIN_ISLAND_REACH_BLOCKS;
                int across = random.nextInt(2 * MAIN_ISLAND_REACH_BLOCKS) - MAIN_ISLAND_REACH_BLOCKS;
                float base = height(fold, axis, along, across);
                String where = axis + "=" + along + " vs " + axis + "=" + (along + width) + ", across " + across
                        + " in " + fold;
                assertTrue(base > NO_ISLAND_HEIGHT, () -> "the main island does not reach " + where);
                assertEquals(base, height(fold, axis, along + width, across), () -> "main island at " + where);
            }
        }
    }

    private static void outerIslandsRepeat(List<WorldFold> folds, Direction.Axis axis) {
        Random random = new Random(SEED);
        for (WorldFold fold : folds) {
            WrapDomain domain = fold.blockDomain(axis);
            WrapDomain other = fold.blockDomain(axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            int hits = 0;
            for (int i = 0; i < SAMPLES; i++) {
                int along = domain.lowerBound - SWEEP_REACH_BLOCKS + random.nextInt(2 * SWEEP_REACH_BLOCKS);
                int across = anyBlock(random, other);
                float base = height(fold, axis, along, across);
                assertEquals(base, height(fold, axis, along + domain.domainLength, across),
                        () -> "outer islands at " + axis + "=" + along + " vs " + axis + "="
                                + (along + domain.domainLength) + ", across " + across + " in " + fold);
                hits += base > NO_ISLAND_HEIGHT ? 1 : 0;
            }

            assertStoodOnIslands(hits, "across the " + axis + " seam of " + fold);
        }
    }

    private static float height(WorldFold fold, Direction.Axis axis, int along, int across) {
        return axis == Direction.Axis.X
                ? PeriodicEndIslands.heightValue(ISLAND_NOISE, fold, along, across)
                : PeriodicEndIslands.heightValue(ISLAND_NOISE, fold, across, along);
    }

    private static int clearOfSeam(Random random, WrapDomain domain) {
        if (!domain.loops()) {
            return anyBlock(random, domain);
        }

        return domain.lowerBound + SWEEP_REACH_BLOCKS + random.nextInt(domain.domainLength - 2 * SWEEP_REACH_BLOCKS);
    }

    private static int anyBlock(Random random, WrapDomain domain) {
        if (!domain.loops()) {
            return random.nextInt(UNBOUNDED_SPAN) - UNBOUNDED_SPAN / 2;
        }

        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    private static void assertStoodOnIslands(int hits, String where) {
        assertTrue(hits > 0, () -> "no sample " + where + " stood on an island, so every comparison read the void");
    }
}
