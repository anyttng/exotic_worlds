package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

class UnfoldedRandomStateTest {
    private static final int WIDTH_BLOCKS = 512;
    private static final int POINTS = 48;
    private static final int VOLUMES = 4;

    private static final int CELL_WIDTH = 4;
    private static final int CELL_HEIGHT = 8;
    private static final int VOLUME_CELLS_XZ = 5;
    private static final int VOLUME_CELLS_Y = 6;

    private static final WorldFold BOUND = torusOfWidth(WIDTH_BLOCKS);

    private static final List<ResourceKey<NoiseGeneratorSettings>> SETTINGS = List.of(
            NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.NETHER, NoiseGeneratorSettings.END);

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void aBoundTransformerLeavesThePointReadVanilla() {
        List<String> broken = new ArrayList<>();
        for (ResourceKey<NoiseGeneratorSettings> key : SETTINGS) {
            NoiseGeneratorSettings settings = settingsOf(key);
            RandomState state = randomState(settings, WorldFolds.NOOP, SEED_BASE);
            Random random = new Random(SEED_BASE);
            for (Map.Entry<String, DensityFunction> field : fieldsOf(state).entrySet()) {
                DensitySampler sampler = state.getSampler(field.getValue());
                int mismatches = 0;
                for (int i = 0; i < POINTS; i++) {
                    int x = random.nextInt(WIDTH_BLOCKS * 4) - WIDTH_BLOCKS * 2;
                    int y = settings.noiseSettings().minY() + random.nextInt(settings.noiseSettings().height());
                    int z = random.nextInt(WIDTH_BLOCKS * 4) - WIDTH_BLOCKS * 2;
                    float vanilla = sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, x, y, z);
                    float bound = GenerationTransformerContext.withTransformer(BOUND,
                            () -> sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, x, y, z));
                    mismatches += Float.floatToIntBits(vanilla) == Float.floatToIntBits(bound) ? 0 : 1;
                }

                collect(broken, mismatches, key, field.getKey(), "sampleValue");
            }
        }

        assertTrue(broken.isEmpty(), "unfolded samplers that change under a bound transformer: " + broken);
    }

    @Test
    void aBoundTransformerLeavesTheVolumeReadVanilla() {
        List<String> broken = new ArrayList<>();
        for (ResourceKey<NoiseGeneratorSettings> key : SETTINGS) {
            NoiseGeneratorSettings settings = settingsOf(key);
            RandomState state = randomState(settings, WorldFolds.NOOP, SEED_BASE);
            Random random = new Random(SEED_BASE);
            for (Map.Entry<String, DensityFunction> field : fieldsOf(state).entrySet()) {
                DensitySampler sampler = state.getSampler(field.getValue());
                int mismatches = 0;
                for (int i = 0; i < VOLUMES; i++) {
                    DensityVolume volume = volumeAt(
                            cellAligned(random, WIDTH_BLOCKS * 4, CELL_WIDTH) - WIDTH_BLOCKS * 2,
                            settings.noiseSettings().minY() + cellAligned(random,
                                    settings.noiseSettings().height() - VOLUME_CELLS_Y * CELL_HEIGHT, CELL_HEIGHT),
                            cellAligned(random, WIDTH_BLOCKS * 4, CELL_WIDTH) - WIDTH_BLOCKS * 2);
                    float[] vanilla = volume(sampler, volume);
                    float[] bound = GenerationTransformerContext.withTransformer(BOUND, () -> volume(sampler, volume));
                    mismatches += mismatchesBetween(vanilla, bound);
                }

                collect(broken, mismatches, key, field.getKey(), "sampleVolume");
            }
        }

        assertTrue(broken.isEmpty(), "unfolded samplers that change under a bound transformer: " + broken);
    }

    private static Map<String, DensityFunction> fieldsOf(RandomState state) {
        NoiseRouter router = state.router;
        Map<String, DensityFunction> fields = new LinkedHashMap<>();
        fields.put("temperature", router.temperature());
        fields.put("vegetation", router.vegetation());
        fields.put("continents", router.continents());
        fields.put("erosion", router.erosion());
        fields.put("depth", router.depth());
        fields.put("ridges", router.ridges());
        fields.put("final_density", router.finalDensity());
        return fields;
    }

    private static int cellAligned(Random random, int span, int cell) {
        return random.nextInt(span / cell) * cell;
    }

    private static DensityVolume volumeAt(int minX, int minY, int minZ) {
        return new DensityVolume(VOLUME_CELLS_XZ, VOLUME_CELLS_Y, VOLUME_CELLS_XZ, minX, minY, minZ,
                CELL_WIDTH, CELL_HEIGHT, CELL_WIDTH);
    }

    private static float[] volume(DensitySampler sampler, DensityVolume volume) {
        DensityBuffer buffer = DensityBuffer.createUnpooled(volume.size());
        sampler.sampleVolume(SamplerContext.EMPTY_UNCACHED, buffer, volume);
        float[] values = new float[volume.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.get(i);
        }

        return values;
    }

    private static int mismatchesBetween(float[] expected, float[] actual) {
        int mismatches = 0;
        for (int i = 0; i < expected.length; i++) {
            mismatches += Float.floatToIntBits(expected[i]) == Float.floatToIntBits(actual[i]) ? 0 : 1;
        }

        return mismatches;
    }

    private static void collect(List<String> broken, int mismatches, ResourceKey<NoiseGeneratorSettings> key,
            String field, String path) {
        if (mismatches > 0) {
            broken.add(key.identifier().getPath() + " " + field + " " + path + ": " + mismatches);
        }
    }
}
