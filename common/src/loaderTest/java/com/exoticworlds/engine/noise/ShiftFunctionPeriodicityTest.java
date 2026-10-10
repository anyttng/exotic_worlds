package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
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

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.TranslationLattice;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ShiftFunctionPeriodicityTest {
    private static final int SAMPLES = 64;
    private static final double MIN_SPREAD = 0.1;

    private record ShiftFunction(String name, DensityFunction function) {
    }

    private static final List<ShiftFunction> SHIFTS = List.of(
            new ShiftFunction("shift", DensityFunctions.shift(NOISE_DATA)),
            new ShiftFunction("shift_a", DensityFunctions.shiftA(NOISE_DATA)),
            new ShiftFunction("shift_b", DensityFunctions.shiftB(NOISE_DATA)));

    private static final int UNINDEXABLE_BASE_OCTAVE = 32;

    private static final Holder<NormalNoise> UNINDEXABLE_NOISE =
            Holder.direct(NormalNoise.createParity(UNINDEXABLE_BASE_OCTAVE, 1.0));

    private static final List<ShiftFunction> UNINDEXABLE_SHIFTS = List.of(
            new ShiftFunction("shift", DensityFunctions.shift(UNINDEXABLE_NOISE)),
            new ShiftFunction("shift_a", DensityFunctions.shiftA(UNINDEXABLE_NOISE)),
            new ShiftFunction("shift_b", DensityFunctions.shiftB(UNINDEXABLE_NOISE)));

    @Test
    void aShiftWhosePeriodOverflowsTheCellIndexSamplesVanillaAtTheFoldedBlock() {
        Random random = new Random(SEED);
        for (ShiftFunction shift : UNINDEXABLE_SHIFTS) {
            DensitySampler vanilla = compileVanilla(shift.function());
            for (WorldFold transformer : WORLDS) {
                DensitySampler folded = compile(shift.function(), transformer);
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
                                shift.name() + " at the folded block in " + transformer + " at (" + block[0] + ", "
                                        + y + ", " + block[1] + ")");
                    }
                }
            }
        }
    }

    @Test
    void everyShiftFunctionAgreesOneWorldWidthApartInX() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.X).domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                    assertEquals(sample(shift, transformer, x, y, z),
                            sample(shift, transformer, x + width, y, z),
                            at(shift, transformer, "x", x, x + width, y, z));
                }
            }
        }
    }

    @Test
    void everyShiftFunctionAgreesOneWorldWidthApartInZ() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.Z).domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                    assertEquals(sample(shift, transformer, x, y, z),
                            sample(shift, transformer, x, y, z + width),
                            at(shift, transformer, "z", z, z + width, x, y));
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

    private static double sample(ShiftFunction shift, WorldFold transformer, int x, int y, int z) {
        return DensityFunctionFixture.sample(compile(shift.function(), transformer), x, y, z);
    }

    private static String at(ShiftFunction shift, WorldFold transformer,
            String axis, int from, int to, int firstOther, int secondOther) {
        return shift.name() + " in " + transformer + " at " + axis + "=" + from + " vs " + axis + "=" + to
                + " (" + firstOther + ", " + secondOther + ")";
    }
}
