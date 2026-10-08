package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Interval;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

class TerrainCeilingCutTest {
    private static final String JAGGEDNESS_PATH = "overworld/jaggedness";

    private static final int HEIGHT = 128;

    private static final int CELL_HEIGHT = 8;

    private static final int SURFACE_Y = 40;

    private static final double SURFACE_RISE_PER_BLOCK = 0.5;

    private static final double BASE_DENSITY = 0.5;

    private static final double BASE_BLOCKS = 40.0;

    private static final double RAMP_BLOCKS = 16.0;

    private static final double PENALTY = 0.25;

    private static final double NOISE_MAX = 1.0;

    private static final int PROBE_Y = 92;

    private static final int PROBE_Z = 0;

    private static final int COLUMNS = 16;

    private static final int KNEE_X = 8;

    private static final int BELOW_KNEE = 2;

    private static final double TOLERANCE = 1.0e-6;

    private static final DensityFunction CEILINGED = TerrainCeiling.withCeiling(router()).finalDensity();

    private static NoiseRouter router() {
        MappedRegistry<DensityFunction> functions =
                new MappedRegistry<>(Registries.DENSITY_FUNCTION, Lifecycle.stable());
        Holder.Reference<DensityFunction> spline = functions.register(
                ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace(JAGGEDNESS_PATH)),
                DensityFunctions.zero(),
                RegistrationInfo.BUILT_IN);
        functions.freeze();
        DensityFunction jaggedness = DensityFunctions.cache(DensityFunctions
                .mul(new DensityFunctions.HolderHolder(spline), DensityFunctions.constant((float) NOISE_MAX)));
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(zero, zero, zero, zero, zero, zero, new RisingSurface(),
                DensityFunctions.add(DensityFunctions.constant((float) BASE_DENSITY), jaggedness));
    }

    private static double densityAt(int blockX, int blockY) {
        return CompiledDensity.valueAt(CEILINGED, blockX, blockY, PROBE_Z);
    }

    private static int ceilingY(int blockX) {
        return (int) (SURFACE_Y + BASE_BLOCKS + SURFACE_RISE_PER_BLOCK * blockX);
    }

    @Test
    void noFourBlockPlateauSurvivesOnTheCut() {
        for (int blockX = 1; blockX < COLUMNS; blockX++) {
            assertNotEquals(densityAt(blockX - 1, PROBE_Y), densityAt(blockX, PROBE_Y),
                    "blockX " + blockX + " carries its neighbour's density unchanged");
        }
    }

    @Test
    void theCutRisesByTheSameStepAtEveryBlock() {
        double step = PENALTY * SURFACE_RISE_PER_BLOCK / RAMP_BLOCKS;

        for (int blockX = 1; blockX < COLUMNS; blockX++) {
            assertEquals(step, densityAt(blockX, PROBE_Y) - densityAt(blockX - 1, PROBE_Y), TOLERANCE,
                    "blockX " + blockX);
        }
    }

    @Test
    void theRampKeepsItsKneeWhereTheCeilingStarts() {
        int knee = ceilingY(KNEE_X);
        assertNotEquals(0, knee % CELL_HEIGHT,
                "a knee on a cell boundary holds wherever the marker sits, and grades nothing");

        assertEquals(BASE_DENSITY, densityAt(KNEE_X, knee), TOLERANCE);
        assertEquals(BASE_DENSITY, densityAt(KNEE_X, knee - BELOW_KNEE), TOLERANCE);
    }

    private record RisingSurface() implements DensityFunction {
        @Override
        public DensitySampler compileSampler(DensityFunction.CompileContext context) {
            return new DensitySampler() {
                @Override
                public void sampleVolume(SamplerContext samplerContext, DensityBuffer outputBuffer,
                        DensityVolume volume) {
                    DensitySampler.sampleVolumeNaive(samplerContext, outputBuffer, volume, this);
                }

                @Override
                public float sampleValue(SamplerContext samplerContext, int blockX, int blockY, int blockZ) {
                    return (float) (SURFACE_Y + Mth.clamp(blockX * SURFACE_RISE_PER_BLOCK, 0.0, HEIGHT));
                }
            };
        }

        @Override
        public DensityFunction rewriteChildren(DfRewriteRule rule) {
            return this;
        }

        @Override
        public Interval range() {
            return Interval.of(SURFACE_Y, SURFACE_Y + HEIGHT);
        }

        @Override
        public @DensityFunction.Axes int domainAxes() {
            return DensityFunction.AXIS_X;
        }

        @Override
        public MapCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }
}
