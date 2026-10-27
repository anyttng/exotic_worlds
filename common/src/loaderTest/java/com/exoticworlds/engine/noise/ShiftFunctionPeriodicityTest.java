package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
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

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.TranslationLattice;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ShiftFunctionPeriodicityTest {
    private static final int SAMPLES = 64;
    private static final double MIN_SPREAD = 0.1;

    private record ShiftFunction(String name, DensityFunction function) {
    }

    private static final List<ShiftFunction> SHIFTS = List.of(
            new ShiftFunction("shift", withLiveNoise(DensityFunctions.shift(NOISE_DATA))),
            new ShiftFunction("shift_a", withLiveNoise(DensityFunctions.shiftA(NOISE_DATA))),
            new ShiftFunction("shift_b", withLiveNoise(DensityFunctions.shiftB(NOISE_DATA))));

    private static final int UNINDEXABLE_FIRST_OCTAVE = 36;

    private static final NormalNoise.NoiseParameters UNINDEXABLE_PARAMETERS =
            new NormalNoise.NoiseParameters(UNINDEXABLE_FIRST_OCTAVE, 1.0);

    private static final List<ShiftFunction> UNINDEXABLE_SHIFTS = List.of(
            new ShiftFunction("shift", withNoiseOf(DensityFunctions.shift(NOISE_DATA), UNINDEXABLE_PARAMETERS)),
            new ShiftFunction("shift_a", withNoiseOf(DensityFunctions.shiftA(NOISE_DATA), UNINDEXABLE_PARAMETERS)),
            new ShiftFunction("shift_b", withNoiseOf(DensityFunctions.shiftB(NOISE_DATA), UNINDEXABLE_PARAMETERS)));

    @Test
    void aShiftWhosePeriodOverflowsTheCellIndexSamplesVanillaAtTheFoldedBlock() {
        Random random = new Random(SEED);
        for (ShiftFunction shift : UNINDEXABLE_SHIFTS) {
            for (WorldFold fold : WORLDS) {
                TranslationLattice lattice = fold.blockLattice();
                int xWidth = fold.blockDomain(Direction.Axis.X).domainLength;
                int zWidth = fold.blockDomain(Direction.Axis.Z).domainLength;
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                    for (int[] block : new int[][] {{x, z}, {x + xWidth, z}, {x, z + zWidth}}) {
                        double expected = shift.function().compute(new DensityFunction.SinglePointContext(
                                lattice.foldX(block[0], block[1]), y, lattice.foldZ(block[1])));
                        assertEquals(expected, sample(shift, fold, block[0], y, block[1]),
                                shift.name() + " at the folded block in " + fold + " at (" + block[0] + ", "
                                        + y + ", " + block[1] + ")");
                    }
                }
            }
        }
    }

    @Test
    void everyShiftFunctionAgreesOneWorldWidthApartInX() {
        Random random = new Random(SEED);
        for (WorldFold fold : WORLDS) {
            int width = fold.blockDomain(Direction.Axis.X).domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                    assertEquals(sample(shift, fold, x, y, z),
                            sample(shift, fold, x + width, y, z),
                            at(shift, fold, "x", x, x + width, y, z));
                }
            }
        }
    }

    @Test
    void everyShiftFunctionAgreesOneWorldWidthApartInZ() {
        Random random = new Random(SEED);
        for (WorldFold fold : WORLDS) {
            int width = fold.blockDomain(Direction.Axis.Z).domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                    assertEquals(sample(shift, fold, x, y, z),
                            sample(shift, fold, x, y, z + width),
                            at(shift, fold, "z", z, z + width, x, y));
                }
            }
        }
    }

    @Test
    void everyShiftFunctionSamplesAFieldThatVaries() {
        Random random = new Random(SEED);
        for (ShiftFunction shift : SHIFTS) {
            double lowest = Double.MAX_VALUE;
            double highest = -Double.MAX_VALUE;
            for (int i = 0; i < SAMPLES; i++) {
                double value = sample(shift, SQUARE,
                        blockIn(random, SQUARE.blockDomain(Direction.Axis.X)), blockY(random),
                        blockIn(random, SQUARE.blockDomain(Direction.Axis.Z)));
                lowest = Math.min(lowest, value);
                highest = Math.max(highest, value);
            }

            assertTrue(highest - lowest > MIN_SPREAD,
                    shift.name() + " sampled a near-constant field, spread " + (highest - lowest));
        }
    }

    private static double sample(ShiftFunction shift, WorldFold fold, int x, int y, int z) {
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(x, y, z);

        return GenerationFoldContext.withFold(fold, () -> shift.function().compute(at));
    }

    private static String at(ShiftFunction shift, WorldFold fold,
            String axis, int from, int to, int firstOther, int secondOther) {
        return shift.name() + " in " + fold + " at " + axis + "=" + from + " vs " + axis + "=" + to
                + " (" + firstOther + ", " + secondOther + ")";
    }
}
