package com.exoticworlds.compat.c2me;

import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SQUARE;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockY;
import static com.exoticworlds.engine.noise.EndIslandFixture.END_ISLANDS;
import static com.exoticworlds.engine.noise.EndIslandFixture.NO_ISLAND_DENSITY;
import static com.exoticworlds.engine.noise.EndIslandFixture.TORUS;
import static com.exoticworlds.engine.noise.EndIslandFixture.acrossTheSeam;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;


import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;

class C2meCompiledDensityFunctionTest {
    private static final int SAMPLES = 16;
    private static final int SEAM_SAMPLES = 256;

    @Test
    void theCompiledClimateFunctionComputesLikeTheVanillaPath() {
        DensityFunction source = C2meCompiledFunctions.climateSource();
        DensityFunction compiled = C2meCompiledFunctions.compileFolded("toroidal_parity", source, SQUARE);
        C2meCompiledFunctions.Points points = C2meCompiledFunctions.Points.over(SQUARE, SAMPLES);

        for (int i = 0; i < SAMPLES; i++) {
            DensityFunction.FunctionContext at = points.forIndex(i);
            double vanilla = GenerationTransformerContext.withTransformer(SQUARE, () -> source.compute(at));
            double emitted = GenerationTransformerContext.withTransformer(SQUARE, () -> compiled.compute(at));

            assertEquals(vanilla, emitted, "at (" + at.blockX() + ", " + at.blockY() + ", " + at.blockZ() + ")");
        }
    }

    @Test
    void theCompiledClimateFunctionFillsAnArrayLikeTheVanillaPath() {
        DensityFunction source = C2meCompiledFunctions.climateSource();
        DensityFunction compiled = C2meCompiledFunctions.compileFolded("toroidal_parity", source, SQUARE);
        C2meCompiledFunctions.Points points = C2meCompiledFunctions.Points.over(SQUARE, SAMPLES);
        double[] vanilla = new double[SAMPLES];
        double[] emitted = new double[SAMPLES];

        GenerationTransformerContext.runWithTransformer(SQUARE, () -> source.fillArray(vanilla, points));
        GenerationTransformerContext.runWithTransformer(SQUARE, () -> compiled.fillArray(emitted, points));

        assertArrayEquals(vanilla, emitted);
    }

    @Test
    void theCompiledEndIslandFunctionRepeatsOneWorldWidthAwayAcrossTheSeam() {
        DensityFunction compiled = C2meCompiledFunctions.compileFolded("toroidal_end_islands", END_ISLANDS, TORUS);
        WrapDomain xDomain = TORUS.blockDomain(Direction.Axis.X);
        Random random = new Random(SEED);
        int hits = 0;

        for (int i = 0; i < SEAM_SAMPLES; i++) {
            int x = acrossTheSeam(random, xDomain);
            int y = blockY(random);
            int z = blockIn(random, TORUS.blockDomain(Direction.Axis.Z));
            DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(x, y, z);
            DensityFunction.FunctionContext lap = new DensityFunction.SinglePointContext(x + xDomain.domainLength, y, z);
            double vanilla = GenerationTransformerContext.withTransformer(TORUS, () -> END_ISLANDS.compute(at));
            double emitted = GenerationTransformerContext.withTransformer(TORUS, () -> compiled.compute(at));
            double emittedLap = GenerationTransformerContext.withTransformer(TORUS, () -> compiled.compute(lap));

            assertEquals(vanilla, emitted, "at (" + x + ", " + y + ", " + z + ")");
            assertEquals(emitted, emittedLap, "at x=" + x + " vs x=" + (x + xDomain.domainLength) + ", z=" + z);
            hits += emitted > NO_ISLAND_DENSITY ? 1 : 0;
        }

        assertTrue(hits > 0, "no sample across the X seam stood on an island, so every comparison read the void");
    }
}
