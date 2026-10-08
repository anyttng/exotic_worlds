package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.NOISE_DATA;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SQUARE;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.compile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.NoiseStack;

class ShiftNoiseSlotScalingTest {
    private static final int SAMPLES = 16;
    private static final int COORDINATE_SPAN = 512;
    private static final double FLAT_SLOT = 0.0;

    private static final NoiseStack STACK =
            FoldedSamplers.stackOf(NOISE_DATA.value().create(new LegacyRandomSource(SEED)));

    private static final DensitySampler SHIFT = compile(DensityFunctions.shift(NOISE_DATA), SQUARE);

    private static final DensitySampler SHIFT_B = compile(DensityFunctions.shiftB(NOISE_DATA), SQUARE);

    @Test
    void shiftNoneYSlotArrivesAtTheNoiseScaled() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            int x = coordinate(random);
            int y = coordinate(random);
            int z = coordinate(random);
            assertEquals(reference(SlotAxes.DEFAULT, x, y * NoiseConstants.SHIFT_SCALE, z),
                    DensityFunctionFixture.sample(SHIFT, x, y, z),
                    at("shift y", x, y, z));
        }
    }

    @Test
    void shiftBSamplesZAndXInItsLoopedSlotsAndAFlatThirdSlot() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            int x = coordinate(random);
            int y = coordinate(random);
            int z = coordinate(random);
            assertEquals(reference(DensityFunctionSlotAxes.SHIFT_B, z, x, FLAT_SLOT),
                    DensityFunctionFixture.sample(SHIFT_B, x, y, z),
                    at("shift_b", x, y, z));
        }
    }

    private static float reference(SlotAxes axes, double first, double second, double third) {
        NoiseFrame frame = new NoiseFrame(axes, NoiseConstants.UNDIVIDED, NoiseConstants.UNDIVIDED,
                GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE);
        float value = PeriodicOctaveSampler.sample(SQUARE, frame, NoiseConstants.SHIFT_SCALE, STACK,
                first, second, third);
        return value * (float) NoiseConstants.SHIFT_AMPLITUDE;
    }

    private static int coordinate(Random random) {
        return random.nextInt(COORDINATE_SPAN) - COORDINATE_SPAN / 2;
    }

    private static String at(String slot, int x, int y, int z) {
        return slot + " at (" + x + ", " + y + ", " + z + ")";
    }
}
