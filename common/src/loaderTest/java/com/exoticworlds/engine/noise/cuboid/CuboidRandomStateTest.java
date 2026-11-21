package com.exoticworlds.engine.noise.cuboid;

import static com.exoticworlds.core.CuboidFixtures.MIN_CHUNK_X;
import static com.exoticworlds.core.CuboidFixtures.MIN_CHUNK_Z;
import static com.exoticworlds.core.CuboidFixtures.cuboidFold;
import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.compileSeparated;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.sample;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.api.v1.gen.GenerationHooks;
import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScanFixture;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class CuboidRandomStateTest {
    private static final String HOOK_KEY = "cuboid_random_state_test";

    private static final int EDGE_CHUNKS = 16;

    private static final int TORUS_WIDTH_BLOCKS = 512;

    private static final int FIRST_OCTAVE = -10;

    private static final Holder<NormalNoise> NOISE_DATA =
            Holder.direct(NormalNoise.createParity(FIRST_OCTAVE, 1.0, 0.25));

    private static final double NEAR_SCALE = 0.75;

    private static final double FAR_SCALE = 0.73;

    private static final double FLAT = 0.0;

    private static final int GRID_STEPS = 32;

    private static final int SAMPLE_Y = 64;

    private static final double OCEAN_GAP = 0.025;

    private static final List<ResourceKey<NoiseGeneratorSettings>> SETTINGS = List.of(
            NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.NETHER, NoiseGeneratorSettings.END);

    private static final AtomicInteger HOOK_RUNS = new AtomicInteger();

    private static final CuboidAtlas ATLAS =
            CuboidAtlas.of(EDGE_CHUNKS, EDGE_CHUNKS, EDGE_CHUNKS, MIN_CHUNK_X, MIN_CHUNK_Z);

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
        GenerationHooks.atRandomState(HOOK_KEY, (randomState, shape, options, seaLevel) -> HOOK_RUNS.incrementAndGet());
    }

    @Test
    void aRandomStateBuildsOverACuboid() {
        WorldFold fold = cuboidFold(ATLAS);
        for (ResourceKey<NoiseGeneratorSettings> key : SETTINGS) {
            RandomState state = randomState(settingsOf(key), fold, SEED_BASE);

            assertSame(fold, ((FoldSource) (Object) state).exotic_worlds$wrappedFold(), key.identifier().getPath());
        }
    }

    @Test
    void twoScalesOfOneNoiseStayApartOnACuboid() {
        DensityFunction gap = DensityFunctions.add(
                DensityFunctions.noise(NOISE_DATA, NEAR_SCALE, FLAT),
                DensityFunctions.mul(DensityFunctions.noise(NOISE_DATA, FAR_SCALE, FLAT),
                        DensityFunctions.constant(-1.0F)));
        DensitySampler sampler = compileSeparated(gap, cuboidFold(ATLAS));

        double widest = 0.0;
        for (CuboidFace face : ATLAS.faces()) {
            widest = Math.max(widest, widestOn(sampler, face));
        }

        assertTrue(widest > OCEAN_GAP, "widest gap between scales " + NEAR_SCALE + " and " + FAR_SCALE
                + " on a cube of " + EDGE_CHUNKS + " chunks: " + widest);
    }

    private static double widestOn(DensitySampler sampler, CuboidFace face) {
        int xStep = face.widthBlocks() / GRID_STEPS;
        int zStep = face.depthBlocks() / GRID_STEPS;
        double widest = 0.0;
        for (int i = 0; i < GRID_STEPS; i++) {
            for (int j = 0; j < GRID_STEPS; j++) {
                widest = Math.max(widest,
                        Math.abs(sample(sampler, face.blockX(i * xStep), SAMPLE_Y, face.blockZ(j * zStep))));
            }
        }

        return widest;
    }

    @Test
    void aCuboidRunsNoRandomStateHook() {
        NoiseGeneratorSettings settings = settingsOf(NoiseGeneratorSettings.OVERWORLD);
        int before = HOOK_RUNS.get();
        randomState(settings, torusOfWidth(TORUS_WIDTH_BLOCKS), SEED_BASE);
        int afterTorus = HOOK_RUNS.get();
        randomState(settings, cuboidFold(ATLAS), SEED_BASE);

        assertEquals(before + 1, afterTorus);
        assertEquals(afterTorus, HOOK_RUNS.get());
    }
}
