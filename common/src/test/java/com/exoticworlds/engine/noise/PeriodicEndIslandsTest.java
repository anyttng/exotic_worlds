package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DistanceMetric;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.generator.EndIslandFunction;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class PeriodicEndIslandsTest {
    private static final long SEED = 0x0153EL;
    private static final int SAMPLES = 512;
    private static final int ISLAND_NOISE_SKIP = 17292;

    private static final int SWEEP_REACH_BLOCKS = 13 * 16;
    private static final int MAIN_ISLAND_REACH_BLOCKS = 96;
    private static final int UNBOUNDED_SPAN = 4096;
    private static final float NO_ISLAND_HEIGHT = -100.0F;
    private static final float MAIN_ISLAND_PEAK_HEIGHT = 100.0F;
    private static final float MAX_HEIGHT_VALUE = 80.0F;
    private static final String ONLY_THE_END_ISLAND_RANDOM = "the End islands draw only the End island random";

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-128, 128, -96, 96)));
    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-128, 128), AxisBounds.Unbounded.INSTANCE)));

    private static final List<WorldFold> WRAPPED_X = List.of(TORUS, CYLINDER);
    private static final List<WorldFold> WRAPPED_Z = List.of(TORUS);

    private static final SimplexNoise ISLAND_NOISE = islandNoise();

    private static final DensityFunction.CompileContext SEEDED = new DensityFunction.CompileContext() {
        @Override
        public Noise createNoiseSampler(Holder<NormalNoise> parameters) {
            throw new UnsupportedOperationException(ONLY_THE_END_ISLAND_RANDOM);
        }

        @Override
        public RandomSource createRandom(Identifier seed) {
            throw new UnsupportedOperationException(ONLY_THE_END_ISLAND_RANDOM);
        }

        @Override
        @Deprecated
        public RandomSource createEndIslandRandom() {
            return new LegacyRandomSource(SEED);
        }
    };

    private static final CoastLiftCache NO_LIFT = new CoastLiftCache() {
        @Override
        public double toroidal$coastLift() {
            return 0.0;
        }

        @Override
        public void toroidal$coastLift(double lift) {
        }
    };

    private static SimplexNoise islandNoise() {
        RandomSource random = new LegacyRandomSource(SEED);
        random.consumeCount(ISLAND_NOISE_SKIP);
        return new SimplexNoise(random, true);
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
                    float folded = PeriodicEndIslands.outerHeightValue(ISLAND_NOISE, fold, x, z);
                    assertEquals(PeriodicEndIslands.outerHeightValue(ISLAND_NOISE, WorldFolds.NOOP, x, z), folded,
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
            DensitySampler vanilla = new EndIslandFunction().compileSampler(SEEDED);
            Random random = new Random(SEED);
            int hits = 0;
            for (int i = 0; i < SAMPLES; i++) {
                int x = random.nextInt(UNBOUNDED_SPAN);
                int z = random.nextInt(UNBOUNDED_SPAN);
                float height = PeriodicEndIslands.outerHeightValue(ISLAND_NOISE, WorldFolds.NOOP, x, z);
                assertEquals(vanilla.sampleValue(SamplerContext.EMPTY_UNCACHED, x, 0, z),
                        PeriodicEndIslands.density(height), () -> "at (" + x + ", " + z + ")");
                hits += height > NO_ISLAND_HEIGHT ? 1 : 0;
            }

            assertStoodOnIslands(hits, "in the non-negative quadrant");
        }
    }

    @Nested
    class SkewedLattice {
        private static final WorldFold SKEWED = new DeckGroupFold(
                FlatShape.latticeTorus(new WorldLoopBounds(-128, 128, -96, 96), 37));

        private static final TranslationLattice LATTICE = SKEWED.blockLattice();

        private static final int[][] COPIES = {
                {LATTICE.x().domainLength, 0},
                {LATTICE.skew(), LATTICE.z().domainLength},
                {LATTICE.skew() - LATTICE.x().domainLength, LATTICE.z().domainLength},
                {-LATTICE.skew(), -LATTICE.z().domainLength}};

        @Test
        void outerIslandsAreTheSameAtEveryDeckCopyAcrossTheSeams() {
            Random random = new Random(SEED);
            int hits = 0;
            for (int i = 0; i < SAMPLES; i++) {
                int x = LATTICE.x().lowerBound - SWEEP_REACH_BLOCKS + random.nextInt(2 * SWEEP_REACH_BLOCKS);
                int z = LATTICE.z().lowerBound - SWEEP_REACH_BLOCKS + random.nextInt(2 * SWEEP_REACH_BLOCKS);
                float base = PeriodicEndIslands.outerHeightValue(ISLAND_NOISE, SKEWED, x, z);
                for (int[] copy : COPIES) {
                    assertEquals(base, PeriodicEndIslands.outerHeightValue(ISLAND_NOISE, SKEWED, x + copy[0],
                            z + copy[1]), () -> "outer islands at (" + x + ", " + z + ") vs its copy " + copy[0]
                                    + ", " + copy[1]);
                }
                hits += base > NO_ISLAND_HEIGHT ? 1 : 0;
            }

            assertStoodOnIslands(hits, "across the seams of a skewed lattice");
        }

        @Test
        void theMainIslandIsTheSameAtEveryDeckCopy() {
            DensitySampler distance = FoldedSamplers.distanceToPoint(new FoldedCompileContext(SEEDED, SKEWED, NO_LIFT),
                    Vec3i.ZERO, DistanceMetric.EUCLIDEAN);
            Random random = new Random(SEED);
            for (int i = 0; i < SAMPLES; i++) {
                int x = random.nextInt(2 * MAIN_ISLAND_REACH_BLOCKS) - MAIN_ISLAND_REACH_BLOCKS;
                int z = random.nextInt(2 * MAIN_ISLAND_REACH_BLOCKS) - MAIN_ISLAND_REACH_BLOCKS;
                float base = distance.sampleValue(SamplerContext.EMPTY_UNCACHED, x, 0, z);
                assertTrue(base < MAIN_ISLAND_PEAK_HEIGHT - NO_ISLAND_HEIGHT,
                        () -> "the main island does not reach (" + x + ", " + z + ")");
                for (int[] copy : COPIES) {
                    assertEquals(base, distance.sampleValue(SamplerContext.EMPTY_UNCACHED, x + copy[0], 0,
                            z + copy[1]), () -> "main island at (" + x + ", " + z + ") vs its copy " + copy[0]
                                    + ", " + copy[1]);
                }
            }
        }
    }

    private static void mainIslandRepeats(List<WorldFold> folds, Direction.Axis axis) {
        Random random = new Random(SEED);
        for (WorldFold fold : folds) {
            int width = fold.blockDomain(axis).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int along = random.nextInt(2 * MAIN_ISLAND_REACH_BLOCKS) - MAIN_ISLAND_REACH_BLOCKS;
                int across = random.nextInt(2 * MAIN_ISLAND_REACH_BLOCKS) - MAIN_ISLAND_REACH_BLOCKS;
                float base = mainIslandHeight(fold, axis, along, across);
                String where = axis + "=" + along + " vs " + axis + "=" + (along + width) + ", across " + across
                        + " in " + fold;
                assertTrue(base > NO_ISLAND_HEIGHT, () -> "the main island does not reach " + where);
                assertEquals(base, mainIslandHeight(fold, axis, along + width, across), () -> "main island at " + where);
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
                ? PeriodicEndIslands.outerHeightValue(ISLAND_NOISE, fold, along, across)
                : PeriodicEndIslands.outerHeightValue(ISLAND_NOISE, fold, across, along);
    }

    private static float mainIslandHeight(WorldFold fold, Direction.Axis axis, int along, int across) {
        DensitySampler distance = FoldedSamplers.distanceToPoint(new FoldedCompileContext(SEEDED, fold, NO_LIFT),
                Vec3i.ZERO, DistanceMetric.EUCLIDEAN);
        float blocks = axis == Direction.Axis.X
                ? distance.sampleValue(SamplerContext.EMPTY_UNCACHED, along, 0, across)
                : distance.sampleValue(SamplerContext.EMPTY_UNCACHED, across, 0, along);
        return Mth.clamp(MAIN_ISLAND_PEAK_HEIGHT - blocks, NO_ISLAND_HEIGHT, MAX_HEIGHT_VALUE);
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
