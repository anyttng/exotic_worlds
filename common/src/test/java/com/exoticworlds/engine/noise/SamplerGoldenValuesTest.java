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
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.periodic.LapFloor;
import com.exoticworlds.engine.noise.periodic.PeriodicLattice;
import com.exoticworlds.engine.noise.periodic.PeriodicNoiseSampler;
import com.exoticworlds.engine.noise.periodic.SimplexLattice;
import com.exoticworlds.engine.noise.periodic.SimplexSlot;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class SamplerGoldenValuesTest {
    private static final long SEED = 0x0153EL;
    private static final int POINTS = 512;
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
    private static final double[] FUDGES = {PerlinMath.NO_FUDGE, 1.0};
    private static final double[] UNFUDGED = {PerlinMath.NO_FUDGE};
    private static final long[] LATTICE_PERIODS = {PeriodicNoiseSampler.UNBOUNDED_PERIOD, 7L, 64L};
    private static final double[] NOISE_FACTORS = {80.0, 400.0, 1.0, 1.0E-7};
    private static final double[] NOISE_LEVELS = {0.0, -0.8};
    private static final double BAMBOO_OFFSET = 0.3;
    private static final int BAMBOO_RATIO = 160;
    private static final int GRASS_BELOW = 5;
    private static final int GRASS_ABOVE = 10;

    private static final NoiseFrame DEFAULT_FRAME = new NoiseFrame(SlotAxes.DEFAULT, NoiseConstants.UNDIVIDED,
            NoiseConstants.UNDIVIDED, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE);
    private static final NoiseFrame SHIFT_B = new NoiseFrame(DensityFunctionSlotAxes.SHIFT_B,
            NoiseConstants.UNDIVIDED, NoiseConstants.UNDIVIDED, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE);
    private static final NoiseFrame DIVIDED = new NoiseFrame(SlotAxes.DEFAULT, 64.0, 16.0,
            GenerationFoldContext.UNDECLARED_VERTICAL_SHARE);

    private static final PerlinNoise PERLIN = new PerlinNoise(new LegacyRandomSource(SEED));
    private static final SimplexNoise SIMPLEX = new SimplexNoise(new LegacyRandomSource(SEED));

    @Test
    void perlinOnTheWorldLatticeKeepsItsValues() {
        assertEquals(0x42E857D7CF0EB022L, perlin(DEFAULT_FRAME, FUDGES));
    }

    @Test
    void perlinOnASlottedLatticeKeepsItsValues() {
        assertEquals(0x88CEAABADDCE7A17L, perlin(SHIFT_B, UNFUDGED));
    }

    @Test
    void perlinOnADividedLatticeKeepsItsValues() {
        assertEquals(0xB3F96402E4F532D9L, perlin(DIVIDED, UNFUDGED));
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
                    hash = mix(hash, Float.floatToRawIntBits(
                            PeriodicNoiseSampler.sampleLattice(PERLIN.perms, xs, ys, zs, xPeriod, zPeriod)));
                }
            }
        }

        assertEquals(0xF37428533FA0EF26L, hash);
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
                            lattice.sample(SIMPLEX.perms, SIMPLEX.offsetX, SIMPLEX.offsetY, x, z)));
                }
            }
        }

        assertEquals(0xB4E16BF76B87D1F3L, hash);
    }

    @Test
    void aNoiseBasedCountKeepsItsValuesThroughOneSlotAcrossFoldsAndFactors() {
        SimplexSlot slot = new SimplexSlot();
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        for (WorldFold fold : SIMPLEX_FOLDS) {
            for (double factor : NOISE_FACTORS) {
                for (int i = 0; i < POINTS; i++) {
                    int x = (int) Math.floor(coord(random));
                    int z = (int) Math.floor(coord(random));
                    hash = mix(hash, FoldedPlacementCounts.noiseBased(slot, fold, x, z, factor, BAMBOO_OFFSET,
                            BAMBOO_RATIO));
                }
            }
        }

        assertEquals(0x89B2F74A0746449FL, hash);
    }

    @Test
    void aThresholdCountKeepsItsValuesAcrossFolds() {
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        for (WorldFold fold : SIMPLEX_FOLDS) {
            for (double level : NOISE_LEVELS) {
                for (int i = 0; i < POINTS; i++) {
                    int x = (int) Math.floor(coord(random));
                    int z = (int) Math.floor(coord(random));
                    hash = mix(hash, FoldedPlacementCounts.noiseThreshold(fold, x, z, level, GRASS_BELOW,
                            GRASS_ABOVE));
                }
            }
        }

        assertEquals(0xA03970CFD032F505L, hash);
    }

    private static long perlin(NoiseFrame frame, double[] fudges) {
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        for (WorldFold fold : PERLIN_FOLDS) {
            for (double scale : PERLIN_SCALES) {
                for (double fudge : fudges) {
                    PeriodicLattice lattice = PeriodicNoiseSampler.lattice(PERLIN.perms, PERLIN.offsetX,
                            PERLIN.offsetY, PERLIN.offsetZ, fudge, fold, frame, scale, LapFloor.of(fold));
                    for (int i = 0; i < POINTS; i++) {
                        double x = coord(random);
                        double y = random.nextInt(384) - 64 + random.nextInt(16) / 16.0;
                        double z = coord(random);
                        hash = mix(hash, Float.floatToRawIntBits(PeriodicNoiseSampler.sample(lattice, x, y, z, y)));
                    }
                }
            }
        }

        return hash;
    }

    private static double coord(Random random) {
        return random.nextInt(4096) - 2048 + random.nextInt(16) / 16.0;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * FNV_PRIME;
    }
}
