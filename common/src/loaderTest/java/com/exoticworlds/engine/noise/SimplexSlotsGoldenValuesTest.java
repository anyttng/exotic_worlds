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

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

class SimplexSlotsGoldenValuesTest {
    private static final long SEED = 0x0153EL;
    private static final int POINTS = 512;
    private static final long FNV_OFFSET = 0xCBF29CE484222325L;
    private static final long FNV_PRIME = 0x100000001B3L;

    private static final WorldLoopBounds SQUARE = ShapeFixtures.loopedBounds(-32, 32, -32, 32);
    private static final List<WorldFold> FOLDS = List.of(
            WorldFolds.of(ShapeFixtures.torus(SQUARE)),
            WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-8, 8, -8, 8))),
            WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-32, 32, 0, 16))),
            WorldFolds.of(ShapeFixtures.cylinder(
                    new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE))),
            WorldFolds.NOOP,
            new DeckGroupFold(FlatShape.latticeTorus(SQUARE, 5), GenerationOptions.DEFAULT),
            new DeckGroupFold(FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-32, 32, 0, 16), -13),
                    GenerationOptions.DEFAULT));
    private static final double[] DIVISORS = {80.0, 400.0, 200.0, 1.0, 1.0E-7};
    private static final double[] SCALES = {1.0 / 1024.0, 0.0125, 0.05, 0.2};
    private static final List<Integer> OCTAVES = List.of(-2, -1, 0);

    @Test
    void biomeInfoKeepsItsValuesAcrossFoldsAndDivisors() {
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        for (WorldFold fold : FOLDS) {
            for (double divisor : DIVISORS) {
                for (int i = 0; i < POINTS; i++) {
                    int x = (int) Math.floor(coord(random));
                    int z = (int) Math.floor(coord(random));
                    double value = GenerationFoldContext.withFold(fold,
                            () -> FoldedBiomeInfo.sample(GenerationFoldContext.context(), fold, divisor, x, z));
                    hash = mix(hash, Double.doubleToRawLongBits(value));
                }
            }
        }

        assertEquals(0x9897A55FCAA085DDL, hash);
    }

    @Test
    void anOctaveNoiseKeepsItsValuesAcrossFoldsAndScales() {
        PerlinSimplexNoise noise = new PerlinSimplexNoise(new LegacyRandomSource(SEED), OCTAVES);
        long hash = FNV_OFFSET;
        Random random = new Random(SEED);
        for (WorldFold fold : FOLDS) {
            for (double scale : SCALES) {
                for (int i = 0; i < POINTS; i++) {
                    double x = coord(random);
                    double z = coord(random);
                    hash = mix(hash, Double.doubleToRawLongBits(sampleAt(noise, fold, scale, x, z)));
                }
            }
        }

        assertEquals(0x29E6A68DA2D825BAL, hash);
    }

    private static double sampleAt(PerlinSimplexNoise noise, WorldFold fold, double scale, double x, double z) {
        return GenerationFoldContext.withFold(fold, () -> {
            try (GenerationFoldContext.Context.ScaleScope scaleScope = GenerationFoldContext.context().withScale(scale)) {
                return noise.getValue(x, z, false);
            }
        });
    }

    private static double coord(Random random) {
        return random.nextInt(4096) - 2048 + random.nextInt(16) / 16.0;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * FNV_PRIME;
    }
}
