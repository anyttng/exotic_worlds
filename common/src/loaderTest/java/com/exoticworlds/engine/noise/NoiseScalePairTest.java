package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.compileSeparated;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.ladderOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class NoiseScalePairTest {
    private static final int FIRST_OCTAVE = -10;

    private static final Holder<NormalNoise> NOISE_DATA =
            Holder.direct(NormalNoise.createParity(FIRST_OCTAVE, 1.0, 0.25));

    private static final double NEAR_SCALE = 0.75;

    private static final double FAR_SCALE = 0.73;

    private static final double FLAT = 0.0;

    private static final double WIDE_SCALE = 2.0;

    private static final double NARROW_SCALE = 1.0;

    private static final int TINY_HALF_CHUNKS = 16;

    private static final int HUGE_HALF_CHUNKS = 256;

    private static final int GRID_STEPS = 64;

    private static final int SAMPLE_Y = 64;

    private static final double OCEAN_GAP = 0.025;

    static List<WorldFold> worlds() {
        return List.of(torus(TINY_HALF_CHUNKS), torus(HUGE_HALF_CHUNKS));
    }

    @ParameterizedTest
    @MethodSource("worlds")
    void twoScalesOfOneNoiseDriftApartAcrossTheLap(WorldFold fold) {
        DensityFunction gap = DensityFunctions.add(
                DensityFunctions.noise(NOISE_DATA, NEAR_SCALE, FLAT),
                DensityFunctions.mul(DensityFunctions.noise(NOISE_DATA, FAR_SCALE, FLAT),
                        DensityFunctions.constant(-1.0F)));
        DensitySampler sampler = compileSeparated(gap, fold);

        int lower = fold.blockDomain(Direction.Axis.X).lowerBound;
        int width = fold.blockDomain(Direction.Axis.X).domainLength;
        int step = width / GRID_STEPS;
        double widest = 0.0;
        for (int i = 0; i < GRID_STEPS; i++) {
            for (int j = 0; j < GRID_STEPS; j++) {
                float value = DensityFunctionFixture.sample(sampler, lower + i * step, SAMPLE_Y, lower + j * step);
                widest = Math.max(widest, Math.abs(value));
            }
        }

        assertTrue(widest > OCEAN_GAP, "widest gap between scales " + NEAR_SCALE + " and " + FAR_SCALE
                + " on a " + width + "-block lap: " + widest);
    }

    @Test
    void scalesThatAlreadyKeepApartAreLeftAsTheyAre() {
        WorldFold fold = torus(HUGE_HALF_CHUNKS);
        DensityFunction pair = DensityFunctions.add(DensityFunctions.noise(NOISE_DATA, WIDE_SCALE, FLAT),
                DensityFunctions.noise(NOISE_DATA, NARROW_SCALE, FLAT));
        NoiseScaleLadder ladder = ladderOf(pair, fold);

        assertEquals(WIDE_SCALE, ladder.separated(NOISE_DATA, WIDE_SCALE));
        assertEquals(NARROW_SCALE, ladder.separated(NOISE_DATA, NARROW_SCALE));
    }

    private static WorldFold torus(int halfChunks) {
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-halfChunks, halfChunks, -halfChunks, halfChunks)));
    }
}
