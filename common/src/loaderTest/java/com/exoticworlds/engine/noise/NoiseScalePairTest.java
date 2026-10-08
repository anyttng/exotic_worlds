package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
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

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class NoiseScalePairTest {
    private static final int FIRST_OCTAVE = -10;

    private static final NormalNoise.NoiseParameters PARAMETERS =
            new NormalNoise.NoiseParameters(FIRST_OCTAVE, DoubleArrayList.of(1.0, 0.25));

    private static final Holder<NormalNoise.NoiseParameters> NOISE_DATA = Holder.direct(PARAMETERS);

    private static final DensityFunction.NoiseHolder NOISE = new DensityFunction.NoiseHolder(
            NOISE_DATA, NormalNoise.create(new LegacyRandomSource(SEED), PARAMETERS));

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
                noise(NEAR_SCALE),
                DensityFunctions.mul(noise(FAR_SCALE), DensityFunctions.constant(-1.0)));
        ladderOf(gap, fold).install();

        int lower = fold.blockDomain(Direction.Axis.X).lowerBound;
        int width = fold.blockDomain(Direction.Axis.X).domainLength;
        int step = width / GRID_STEPS;
        double widest = 0.0;
        for (int i = 0; i < GRID_STEPS; i++) {
            for (int j = 0; j < GRID_STEPS; j++) {
                DensityFunction.FunctionContext at =
                        new DensityFunction.SinglePointContext(lower + i * step, SAMPLE_Y, lower + j * step);
                double value = GenerationTransformerContext.withTransformer(fold, () -> gap.compute(at));
                widest = Math.max(widest, Math.abs(value));
            }
        }

        assertTrue(widest > OCEAN_GAP, "widest gap between scales " + NEAR_SCALE + " and " + FAR_SCALE
                + " on a " + width + "-block lap: " + widest);
    }

    @Test
    void scalesThatAlreadyKeepApartAreLeftAsTheyAre() {
        WorldFold fold = torus(HUGE_HALF_CHUNKS);
        DensityFunction pair = DensityFunctions.add(noise(WIDE_SCALE), noise(NARROW_SCALE));
        NoiseScaleLadder ladder = ladderOf(pair, fold);

        assertEquals(WIDE_SCALE, ladder.separated(NOISE.noise(), WIDE_SCALE));
        assertEquals(NARROW_SCALE, ladder.separated(NOISE.noise(), NARROW_SCALE));
    }

    private static DensityFunction noise(double xzScale) {
        return DensityFunctions.noise(NOISE_DATA, xzScale, FLAT).mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                return input;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                return NOISE;
            }
        });
    }

    private static WorldFold torus(int halfChunks) {
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-halfChunks, halfChunks, -halfChunks, halfChunks)));
    }
}
