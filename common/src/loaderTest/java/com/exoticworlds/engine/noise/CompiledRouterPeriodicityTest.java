package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.cylinderOfWidth;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

class CompiledRouterPeriodicityTest {
    private static final int WIDTH_BLOCKS = 512;
    private static final int POINTS = 48;
    private static final int VOLUMES = 4;

    private static final int CELL_WIDTH = 4;
    private static final int CELL_HEIGHT = 8;
    private static final int VOLUME_CELLS_XZ = 5;
    private static final int VOLUME_CELLS_Y = 6;

    private static final List<ResourceKey<NoiseGeneratorSettings>> SETTINGS = List.of(
            NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.LARGE_BIOMES, NoiseGeneratorSettings.AMPLIFIED,
            NoiseGeneratorSettings.NETHER, NoiseGeneratorSettings.END);

    private record Probe(String name, WorldFold fold, List<Direction.Axis> axes) {
    }

    private static final List<Probe> PROBES = List.of(
            new Probe("torus", torusOfWidth(WIDTH_BLOCKS), List.of(Direction.Axis.X, Direction.Axis.Z)),
            new Probe("cylinder", cylinderOfWidth(WIDTH_BLOCKS), List.of(Direction.Axis.X)));

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void everyRouterSamplerRepeatsOneLapAwayPointByPoint() {
        List<String> broken = new ArrayList<>();
        for (Probe probe : PROBES) {
            for (ResourceKey<NoiseGeneratorSettings> key : SETTINGS) {
                NoiseGeneratorSettings settings = settingsOf(key);
                RandomState state = randomState(settings, probe.fold(), SEED_BASE);
                Random random = new Random(SEED_BASE);
                for (Map.Entry<String, DensityFunction> field : fieldsOf(settings, state).entrySet()) {
                    DensitySampler sampler = state.getSampler(field.getValue());
                    for (Direction.Axis axis : probe.axes()) {
                        int mismatches = 0;
                        for (int i = 0; i < POINTS; i++) {
                            int x = random.nextInt(WIDTH_BLOCKS) - WIDTH_BLOCKS / 2;
                            int y = settings.noiseSettings().minY() + random.nextInt(settings.noiseSettings().height());
                            int z = random.nextInt(WIDTH_BLOCKS) - WIDTH_BLOCKS / 2;
                            float here = sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, x, y, z);
                            float lapAway = axis == Direction.Axis.X
                                    ? sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, x + WIDTH_BLOCKS, y, z)
                                    : sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, x, y, z + WIDTH_BLOCKS);
                            mismatches += Float.floatToIntBits(here) == Float.floatToIntBits(lapAway) ? 0 : 1;
                        }

                        collect(broken, mismatches, probe, key, field.getKey(), axis, "sampleValue");
                    }
                }
            }
        }

        assertTrue(broken.isEmpty(), "compiled samplers that do not repeat one lap away: " + broken);
    }

    @Test
    void everyRouterSamplerRepeatsOneLapAwayOverAVolume() {
        List<String> broken = new ArrayList<>();
        for (Probe probe : PROBES) {
            for (ResourceKey<NoiseGeneratorSettings> key : SETTINGS) {
                NoiseGeneratorSettings settings = settingsOf(key);
                RandomState state = randomState(settings, probe.fold(), SEED_BASE);
                Random random = new Random(SEED_BASE);
                for (Map.Entry<String, DensityFunction> field : fieldsOf(settings, state).entrySet()) {
                    DensitySampler sampler = state.getSampler(field.getValue());
                    for (Direction.Axis axis : probe.axes()) {
                        int mismatches = 0;
                        for (int i = 0; i < VOLUMES; i++) {
                            int minX = cellAligned(random, WIDTH_BLOCKS, CELL_WIDTH) - WIDTH_BLOCKS / 2;
                            int minY = settings.noiseSettings().minY() + cellAligned(random,
                                    settings.noiseSettings().height() - VOLUME_CELLS_Y * CELL_HEIGHT, CELL_HEIGHT);
                            int minZ = cellAligned(random, WIDTH_BLOCKS, CELL_WIDTH) - WIDTH_BLOCKS / 2;
                            float[] here = volume(sampler, minX, minY, minZ);
                            float[] lapAway = axis == Direction.Axis.X
                                    ? volume(sampler, minX + WIDTH_BLOCKS, minY, minZ)
                                    : volume(sampler, minX, minY, minZ + WIDTH_BLOCKS);
                            mismatches += mismatchesBetween(here, lapAway);
                        }

                        collect(broken, mismatches, probe, key, field.getKey(), axis, "sampleVolume");
                    }
                }
            }
        }

        assertTrue(broken.isEmpty(), "compiled samplers that do not repeat one lap away: " + broken);
    }

    @Test
    void theVolumeReadsWhatThePointReads() {
        NoiseGeneratorSettings settings = settingsOf(NoiseGeneratorSettings.OVERWORLD);
        RandomState state = randomState(settings, torusOfWidth(WIDTH_BLOCKS), SEED_BASE);
        DensitySampler sampler = state.getSampler(state.router.continents());
        int minY = settings.seaLevel() - settings.seaLevel() % CELL_HEIGHT;
        float[] values = volume(sampler, 0, minY, 0);
        DensityVolume volume = volumeAt(0, minY, 0);

        assertEquals(sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, CELL_WIDTH, minY, CELL_WIDTH),
                values[volume.indexUnchecked(1, 0, 1)]);
    }

    private static Map<String, DensityFunction> fieldsOf(NoiseGeneratorSettings settings, RandomState state) {
        NoiseRouter router = state.router;
        Map<String, DensityFunction> fields = new LinkedHashMap<>();
        fields.put("temperature", router.temperature());
        fields.put("vegetation", router.vegetation());
        fields.put("continents", router.continents());
        fields.put("erosion", router.erosion());
        fields.put("depth", router.depth());
        fields.put("ridges", router.ridges());
        fields.put("chunk_surface_level", router.chunkSurfaceLevel());
        fields.put("final_density", router.finalDensity());
        settings.aquifers().ifPresent(aquifer -> addAquifer(fields, aquifer));
        return fields;
    }

    private static void addAquifer(Map<String, DensityFunction> fields, Aquifer.Config aquifer) {
        fields.put("aquifer_barrier", aquifer.barrierNoise());
        fields.put("aquifer_floodedness", aquifer.fluidLevelFloodednessNoise());
        fields.put("aquifer_spread", aquifer.fluidLevelSpreadNoise());
        fields.put("aquifer_lava", aquifer.lavaNoise());
        fields.put("aquifer_exclusion", aquifer.exclusion());
        fields.put("aquifer_surface_level", aquifer.surfaceLevel());
    }

    private static int cellAligned(Random random, int span, int cell) {
        return random.nextInt(span / cell) * cell;
    }

    private static DensityVolume volumeAt(int minX, int minY, int minZ) {
        return new DensityVolume(VOLUME_CELLS_XZ, VOLUME_CELLS_Y, VOLUME_CELLS_XZ, minX, minY, minZ,
                CELL_WIDTH, CELL_HEIGHT, CELL_WIDTH);
    }

    private static float[] volume(DensitySampler sampler, int minX, int minY, int minZ) {
        DensityVolume volume = volumeAt(minX, minY, minZ);
        DensityBuffer buffer = DensityBuffer.createUnpooled(volume.size());
        sampler.sampleVolume(SamplerContext.EMPTY_UNCACHED, buffer, volume);
        float[] values = new float[volume.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.get(i);
        }

        return values;
    }

    private static int mismatchesBetween(float[] here, float[] lapAway) {
        int mismatches = 0;
        for (int i = 0; i < here.length; i++) {
            mismatches += Float.floatToIntBits(here[i]) == Float.floatToIntBits(lapAway[i]) ? 0 : 1;
        }

        return mismatches;
    }

    private static void collect(List<String> broken, int mismatches, Probe probe,
            ResourceKey<NoiseGeneratorSettings> key, String field, Direction.Axis axis, String path) {
        if (mismatches > 0) {
            broken.add(probe.name() + " " + key.identifier().getPath() + " " + field + " " + path + " along "
                    + axis.getName() + ": " + mismatches);
        }
    }
}
