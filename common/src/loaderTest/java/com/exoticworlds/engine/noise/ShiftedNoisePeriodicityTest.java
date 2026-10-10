package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.NOISE_DATA;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SQUARE;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.WORLDS;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockY;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.compile;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.compileVanilla;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.TranslationLattice;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ShiftedNoisePeriodicityTest {
    private static final int SAMPLES = 64;
    private static final double MIN_WARPED_SHARE = 0.9;

    private static final DensityFunction WARPED = DensityFunctions.shiftedNoise2d(
            DensityFunctions.shiftA(NOISE_DATA), DensityFunctions.shiftB(NOISE_DATA), CLIMATE_XZ_SCALE, NOISE_DATA);

    private static final DensityFunction UNWARPED = DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.zero(), CLIMATE_XZ_SCALE, NOISE_DATA);

    private static final float UNLAPPABLE_SHIFT = 1.01F;

    private static final double UNLAPPABLE_XZ_SCALE = 0x1p-52;

    private static final DensityFunction UNLAPPABLE_WARP = DensityFunctions.shiftedNoise2d(
            DensityFunctions.constant(UNLAPPABLE_SHIFT), DensityFunctions.constant(UNLAPPABLE_SHIFT),
            UNLAPPABLE_XZ_SCALE, NOISE_DATA);

    private static final int UNINDEXABLE_FIRST_OCTAVE = 88;

    private static final double UNINDEXABLE_XZ_SCALE = 0x1p-25;

    private static final float LAPPABLE_SHIFT = 0.982F;

    private static final Holder<NormalNoise> UNINDEXABLE_NOISE =
            Holder.direct(NormalNoise.createParity(UNINDEXABLE_FIRST_OCTAVE, 1.0));

    private static final DensityFunction UNINDEXABLE_PLAIN = DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.zero(), UNINDEXABLE_XZ_SCALE, UNINDEXABLE_NOISE);

    private static final DensityFunction UNINDEXABLE_WARPED = DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.constant(LAPPABLE_SHIFT), UNINDEXABLE_XZ_SCALE,
            UNINDEXABLE_NOISE);

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

    private static void assertVanillaAtFoldedBlock(DensityFunction function) {
        DensitySampler vanilla = compileVanilla(function);
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            DensitySampler folded = compile(function, transformer);
            TranslationLattice lattice = transformer.blockLattice();
            int xWidth = transformer.blockDomain(Direction.Axis.X).domainLength;
            int zWidth = transformer.blockDomain(Direction.Axis.Z).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                int y = blockY(random);
                int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                for (int[] block : new int[][] {{x, z}, {x + xWidth, z}, {x, z + zWidth}}) {
                    float expected = DensityFunctionFixture.sample(vanilla,
                            lattice.foldX(block[0], block[1]), y, lattice.foldZ(block[1]));
                    assertEquals(expected, DensityFunctionFixture.sample(folded, block[0], y, block[1]),
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
        return DensityFunctionFixture.sample(compile(function, transformer), x, y, z);
    }

    private static String at(WorldFold transformer, String axis, int from, int to, int firstOther, int secondOther) {
        return "shifted_noise in " + transformer + " at " + axis + "=" + from + " vs " + axis + "=" + to
                + " (" + firstOther + ", " + secondOther + ")";
    }
}
