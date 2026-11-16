package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

class SamplerGoldenValuesTest {
    private static final long SEED = 0x0153EL;
    private static final int POINTS = 512;
    private static final int PERMUTATION_COUNT = 256;
    private static final int SIMPLEX_PERMUTATION_COUNT = 512;
    private static final long FNV_OFFSET = 0xCBF29CE484222325L;
    private static final long FNV_PRIME = 0x100000001B3L;

    private static final WorldLoopBounds SQUARE = ShapeFixtures.loopedBounds(-32, 32, -32, 32);
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(SQUARE));
    private static final WorldFold SMALL = WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-8, 8, -8, 8)));
    private static final WorldFold UNEVEN = WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-32, 32, 0, 16)));
    private static final WorldFold CYLINDER = WorldFolds.of(ShapeFixtures.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE)));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(SQUARE, 5),
            GenerationOptions.DEFAULT);
    private static final WorldFold SKEWED_BACK = new DeckGroupFold(
            FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-32, 32, 0, 16), -13), GenerationOptions.DEFAULT);

    private static final List<WorldFold> PERLIN_FOLDS = List.of(TORUS, UNEVEN, CYLINDER, SKEWED, SKEWED_BACK);
    private static final List<WorldFold> SIMPLEX_FOLDS = List.of(TORUS, SMALL, UNEVEN, CYLINDER, WorldFolds.NOOP,
            SKEWED, SKEWED_BACK);

    private static final double[] PERLIN_SCALES = {1.0 / 2048.0, 0.0125, 0.05, 0.25, 1.0};
    private static final double[] SIMPLEX_SCALES = {1.0 / 1024.0, 0.0125, 0.05, 0.09, 0.125, 0.2, 1.0};
    private static final double[][] FUDGES = {{0.0, 0.0}, {0.25, 0.6}};
    private static final double[][] UNFUDGED = {{0.0, 0.0}};
    private static final long[] LATTICE_PERIODS = {PeriodicNoiseSampler.UNBOUNDED_PERIOD, 7L, 64L};
    private static final double X_DIVISOR = 64.0;
    private static final double Z_DIVISOR = 16.0;

    private static final double X_OFFSET = 37.25;
    private static final double Y_OFFSET = 11.5;
    private static final double Z_OFFSET = 101.75;
    private static final byte[] PERLIN_PERMUTATIONS = perlinPermutations();
    private static final int[] SIMPLEX_PERMUTATIONS = simplexPermutations();

    @Test
    void perlinOnTheWorldLatticeKeepsItsValues() {
        assertEquals(0x911482D872A7338AL, perlin(SlotAxes.DEFAULT, 1.0, 1.0, FUDGES));
    }

    @Test
    void perlinOnASlottedLatticeKeepsItsValues() {
        assertEquals(0x5A0DD8E2F24E9B8BL, perlin(DensityFunctionSlotAxes.SHIFT_B, 1.0, 1.0, UNFUDGED));
    }

    @Test
    void perlinOnADividedLatticeKeepsItsValues() {
        assertEquals(0xD6B482C95500CA16L, perlin(SlotAxes.DEFAULT, X_DIVISOR, Z_DIVISOR, UNFUDGED));
    }

    @Test
    void aBareLatticeKeepsItsValues() {
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        for (long xPeriod : LATTICE_PERIODS) {
            for (long zPeriod : LATTICE_PERIODS) {
                for (int i = 0; i < POINTS; i++) {
                    double xs = coord(random) * 0.1;
                    double ys = random.nextDouble() * 32.0 - 16.0;
                    double zs = coord(random) * 0.1;
                    hash = mix(hash, Double.doubleToRawLongBits(
                            PeriodicNoiseSampler.sampleLattice(PERLIN_PERMUTATIONS, xs, ys, zs, xPeriod, zPeriod)));
                }
            }
        }

        assertEquals(0x7EA2E9475BBA8D6CL, hash);
    }

    @Test
    void simplexKeepsItsValues() {
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        for (WorldFold fold : SIMPLEX_FOLDS) {
            for (double scale : SIMPLEX_SCALES) {
                SimplexLattice lattice = SimplexLattice.of(fold, scale);
                for (int i = 0; i < POINTS; i++) {
                    double x = coord(random);
                    double z = coord(random);
                    hash = mix(hash, Double.doubleToRawLongBits(
                            lattice.sample(SIMPLEX_PERMUTATIONS, X_OFFSET, Z_OFFSET, x, z)));
                }
            }
        }

        assertEquals(0x9343357438659E83L, hash);
    }

    private static long perlin(SlotAxes axes, double xDivisor, double zDivisor, double[][] fudges) {
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        GenerationFoldContext.Context context = GenerationFoldContext.context();
        for (WorldFold fold : PERLIN_FOLDS) {
            for (double scale : PERLIN_SCALES) {
                for (double[] fudge : fudges) {
                    try (GenerationFoldContext.Context.BindingScope _ = context.bind(fold, axes, scale,
                            GenerationFoldContext.UNDECLARED_VERTICAL_SHARE);
                            GenerationFoldContext.Context.DivisorScope _ = context.withDivisors(xDivisor, zDivisor)) {
                        PeriodicLattice lattice = PeriodicNoiseSampler.lattice(PERLIN_PERMUTATIONS, X_OFFSET,
                                Y_OFFSET, Z_OFFSET, fold, context, LapFloor.of(fold));
                        hash = perlinPoints(hash, random, lattice, fudge);
                    }
                }
            }
        }

        return hash;
    }

    private static long perlinPoints(long start, Random random, PeriodicLattice lattice, double[] fudge) {
        long hash = start;
        for (int i = 0; i < POINTS; i++) {
            double x = coord(random);
            double y = random.nextInt(384) - 64 + random.nextInt(16) / 16.0;
            double z = coord(random);
            hash = mix(hash, Double.doubleToRawLongBits(
                    PeriodicNoiseSampler.sample(lattice, x, y, z, fudge[0], fudge[1])));
        }

        return hash;
    }

    private static byte[] perlinPermutations() {
        Random random = new Random(SEED);
        byte[] permutations = new byte[PERMUTATION_COUNT];
        for (int i = 0; i < PERMUTATION_COUNT; i++) {
            permutations[i] = (byte) i;
        }

        for (int i = PERMUTATION_COUNT - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            byte swap = permutations[i];
            permutations[i] = permutations[j];
            permutations[j] = swap;
        }

        return permutations;
    }

    private static int[] simplexPermutations() {
        byte[] bytes = perlinPermutations();
        int[] permutations = new int[SIMPLEX_PERMUTATION_COUNT];
        for (int i = 0; i < SIMPLEX_PERMUTATION_COUNT; i++) {
            permutations[i] = bytes[i % PERMUTATION_COUNT] & 0xFF;
        }

        return permutations;
    }

    private static double coord(Random random) {
        return random.nextInt(4096) - 2048 + random.nextInt(16) / 16.0;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * FNV_PRIME;
    }
}
