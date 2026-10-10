package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.NOISE_DATA;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SQUARE;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.WORLDS;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockY;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.withLiveNoise;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.withNoiseOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.TranslationLattice;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ShiftedNoisePeriodicityTest {
    private static final int SAMPLES = 64;
    private static final double MIN_WARPED_SHARE = 0.9;

    private static final DensityFunction WARPED = withLiveNoise(DensityFunctions.shiftedNoise2d(
            DensityFunctions.shiftA(NOISE_DATA), DensityFunctions.shiftB(NOISE_DATA), CLIMATE_XZ_SCALE, NOISE_DATA));

    private static final DensityFunction UNWARPED = withLiveNoise(DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.zero(), CLIMATE_XZ_SCALE, NOISE_DATA));

    private static final float UNLAPPABLE_SHIFT = 1.01F;

    private static final double UNLAPPABLE_XZ_SCALE = 0x1p-52;

    private static final DensityFunction UNLAPPABLE_WARP = withLiveNoise(DensityFunctions.shiftedNoise2d(
            DensityFunctions.constant(UNLAPPABLE_SHIFT), DensityFunctions.constant(UNLAPPABLE_SHIFT),
            UNLAPPABLE_XZ_SCALE, NOISE_DATA));

    private static final int UNINDEXABLE_FIRST_OCTAVE = 88;

    private static final double UNINDEXABLE_XZ_SCALE = 0x1p-25;

    private static final float LAPPABLE_SHIFT = 0.982F;

    private static final NormalNoise.NoiseParameters UNINDEXABLE_PARAMETERS =
            new NormalNoise.NoiseParameters(UNINDEXABLE_FIRST_OCTAVE, 1.0);

    private static final DensityFunction UNINDEXABLE_PLAIN = withNoiseOf(DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.zero(), UNINDEXABLE_XZ_SCALE, NOISE_DATA),
            UNINDEXABLE_PARAMETERS);

    private static final DensityFunction UNINDEXABLE_WARPED = withNoiseOf(DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.constant(LAPPABLE_SHIFT), UNINDEXABLE_XZ_SCALE, NOISE_DATA),
            UNINDEXABLE_PARAMETERS);

    private static final DensityFunction UNINDEXABLE_NOISE = withNoiseOf(
            DensityFunctions.noise(NOISE_DATA, UNINDEXABLE_XZ_SCALE, UNINDEXABLE_XZ_SCALE), UNINDEXABLE_PARAMETERS);

    @Test
    void aWarpPastTheLapRangeSamplesVanillaAtTheFoldedBlock() {
        assertVanillaAtFoldedBlock(UNLAPPABLE_WARP);
    }

    @Test
    void aPlainNoiseWhosePeriodOverflowsTheCellIndexSamplesVanillaAtTheFoldedBlock() {
        assertVanillaAtFoldedBlock(UNINDEXABLE_PLAIN);
    }

    @Test
    void aWarpedNoiseWhosePeriodOverflowsTheCellIndexSamplesVanillaAtTheFoldedBlock() {
        assertVanillaAtFoldedBlock(UNINDEXABLE_WARPED);
    }

    @Test
    void aNoiseWhosePeriodOverflowsTheCellIndexSamplesVanillaAtTheFoldedBlock() {
        assertVanillaAtFoldedBlock(UNINDEXABLE_NOISE);
    }

    private static void assertVanillaAtFoldedBlock(DensityFunction function) {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            TranslationLattice lattice = transformer.blockLattice();
            int xWidth = transformer.blockDomain(Direction.Axis.X).domainLength;
            int zWidth = transformer.blockDomain(Direction.Axis.Z).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                int y = blockY(random);
                int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                for (int[] block : new int[][] {{x, z}, {x + xWidth, z}, {x, z + zWidth}}) {
                    double expected = function.compute(new DensityFunction.SinglePointContext(
                            lattice.foldX(block[0], block[1]), y, lattice.foldZ(block[1])));
                    assertEquals(expected, sample(function, transformer, block[0], y, block[1]),
                            "vanilla at the folded block in " + transformer + " at (" + block[0] + ", " + y + ", "
                                    + block[1] + ")");
                }
            }
        }
    }

    @Test
    void warpedNoiseAgreesOneWorldWidthApartInX() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.X).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                int y = blockY(random);
                int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                assertEquals(sample(WARPED, transformer, x, y, z), sample(WARPED, transformer, x + width, y, z),
                        at(transformer, "x", x, x + width, y, z));
            }
        }
    }

    @Test
    void warpedNoiseAgreesOneWorldWidthApartInZ() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.Z).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                int y = blockY(random);
                int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                assertEquals(sample(WARPED, transformer, x, y, z), sample(WARPED, transformer, x, y, z + width),
                        at(transformer, "z", z, z + width, x, y));
            }
        }
    }

    @Test
    void horizontalShiftMovesTheSample() {
        Random random = new Random(SEED);
        int moved = 0;
        for (int i = 0; i < SAMPLES; i++) {
            int x = blockIn(random, SQUARE.blockDomain(Direction.Axis.X));
            int y = blockY(random);
            int z = blockIn(random, SQUARE.blockDomain(Direction.Axis.Z));
            if (sample(WARPED, SQUARE, x, y, z) != sample(UNWARPED, SQUARE, x, y, z)) {
                moved++;
            }
        }

        assertTrue(moved >= SAMPLES * MIN_WARPED_SHARE,
                "the horizontal shift moved only " + moved + " of " + SAMPLES + " samples");
    }

    private static double sample(DensityFunction function, WorldFold transformer, int x, int y, int z) {
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(x, y, z);

        return GenerationTransformerContext.withTransformer(transformer, () -> function.compute(at));
    }

    private static String at(WorldFold transformer, String axis, int from, int to, int firstOther, int secondOther) {
        return "shifted_noise in " + transformer + " at " + axis + "=" + from + " vs " + axis + "=" + to
                + " (" + firstOther + ", " + secondOther + ")";
    }
}
