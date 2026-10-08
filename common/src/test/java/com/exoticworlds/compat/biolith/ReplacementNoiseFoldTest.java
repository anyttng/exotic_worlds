package com.exoticworlds.compat.biolith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.terraformersmc.biolith.impl.noise.OpenSimplexNoise2;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ReplacementNoiseFoldTest {
    private static final long WORLD_SEED = 0x0153EL;

    private static final double[] OVERWORLD_WEIGHTS = {1.0, 1.0 / 8.0, 1.0 / 16.0, 1.0 / 32.0};

    private static final double[] OVERWORLD_SCALE_QUARTS = {1024.0, 256.0, 64.0, 16.0};

    private static final double SUM_DIVISOR = 1.21875;

    private static final double[] NETHER_WEIGHTS = {1.0, 1.0 / 16.0, 1.0 / 32.0};

    private static final double[] NETHER_SCALE_QUARTS = {512.0, 128.0, 64.0, 16.0, 4.0};

    private static final int[] NETHER_HORIZONTAL_SCALE_SLOTS = {0, 2, 3};

    private static final int[] NETHER_VERTICAL_SCALE_SLOTS = {1, 3, 4};

    private static final double NETHER_SUM_DIVISOR = 1.09375;

    private static final int NETHER_QUART_HEIGHT = 32;

    private static final int NETHER_SAMPLE_QUART_Y = 16;

    private static final int COLUMNS_PER_AXIS = 16;

    private static final double REGION_SIZE_RATIO = 1.0;

    private static final double REGION_TOLERANCE = 0.1;

    private static final int REGION_LINES = 384;

    private static final int OVERWORLD_LINE_SAMPLES = 1024;

    private static final int OVERWORLD_LINE_STRIDE_QUARTS = 8;

    private static final int NETHER_LINE_SAMPLES = 2048;

    private static final int NETHER_ROW_STRIDE_QUARTS = 4;

    private static final int NETHER_COLUMN_STRIDE_QUARTS = 1;

    private static final int LINE_QUART_Y_BOUND = 4096;

    private static final double NORMALIZE_GAIN = 0.5375;

    private static final double NORMALIZE_OFFSET = 0.5;

    private static final int SAMPLES_PER_AXIS = 96;

    private static final int PERIODICITY_SAMPLES = 512;

    private static final double SPREAD_RATIO_TOLERANCE = 0.25;

    private static final double HALF = 0.5;

    private static final double SPLIT_TOLERANCE = 0.15;

    private static final int TINY_CHUNK_MIN = -16;

    private static final int TINY_CHUNK_MAX = 16;

    private static final int UNFLOORED_CHUNK_MIN = -2048;

    private static final int UNFLOORED_CHUNK_MAX = 2048;

    private static final WorldFold TINY = torus(TINY_CHUNK_MIN, TINY_CHUNK_MAX);

    private static final WorldFold HUGE = torus(-256, 256);

    private static final WorldFold NETHER_SCALED = torus(-32, 32);

    private static final WorldFold UNFLOORED = torus(UNFLOORED_CHUNK_MIN, UNFLOORED_CHUNK_MAX);

    private static final double CLIMATE_XZ_SCALE = 0.25;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final double FACTOR_TOLERANCE = 1.0E-9;

    private static final double NO_COMPRESSION = 1.0;

    private static final List<ClimateScale> COMPRESSING_SCALES = List.of(ClimateScale.AUTO, ClimateScale.STRONG);

    private static WorldFold torus(int chunkMin, int chunkMax) {
        return torus(chunkMin, chunkMax, GenerationOptions.DEFAULT);
    }

    private static WorldFold torus(int chunkMin, int chunkMax, GenerationOptions options) {
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(chunkMin, chunkMax, chunkMin, chunkMax)), options);
    }

    private static WorldFold compressed(int chunkMin, int chunkMax, ClimateScale scale) {
        return torus(chunkMin, chunkMax, GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, scale));
    }

    private static final class VanillaWorldgen {
        private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createWorldLookup();

        private static final DensityFunction OVERWORLD_TEMPERATURE = temperatureOf(NoiseGeneratorSettings.OVERWORLD);

        private static final DensityFunction NETHER_TEMPERATURE = temperatureOf(NoiseGeneratorSettings.NETHER);

        static DensityFunction temperatureOf(ResourceKey<NoiseGeneratorSettings> settings) {
            return LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings).value().noiseRouter()
                    .temperature();
        }

        static double temperatureFactor(WorldFold transformer, ResourceKey<NormalNoise> temperature) {
            return ClimateCompression.factor(transformer,
                    LOOKUP.lookupOrThrow(Registries.NOISE).getOrThrow(temperature), CLIMATE_XZ_SCALE,
                    HORIZONTAL_SHARE);
        }
    }

    private static void assertTakesTheTemperatureFactor(ResourceKey<NoiseGeneratorSettings> settings,
            ResourceKey<NormalNoise> temperature) {
        for (ClimateScale scale : COMPRESSING_SCALES) {
            WorldFold transformer = compressed(TINY_CHUNK_MIN, TINY_CHUNK_MAX, scale);
            assertEquals(VanillaWorldgen.temperatureFactor(transformer, temperature),
                    fold().compression(transformer, VanillaWorldgen.temperatureOf(settings)), FACTOR_TOLERANCE,
                    settings.identifier() + " " + scale.mode().getSerializedName() + " on a tiny world");
        }
    }

    private static ReplacementNoiseFold fold() {
        return new ReplacementNoiseFold(OVERWORLD_WEIGHTS);
    }

    private static double folded(ReplacementNoiseFold fold, WorldFold transformer, int quartX, int quartZ) {
        double[] sum = new double[1];
        GenerationTransformerContext.withTransformer(transformer,
                () -> sum[0] = fold.sum(WORLD_SEED, OVERWORLD_SCALE_QUARTS, quartX, quartZ,
                        VanillaWorldgen.OVERWORLD_TEMPERATURE));
        return sum[0];
    }

    private static double normalize(double sum) {
        return Mth.clamp(sum / SUM_DIVISOR * NORMALIZE_GAIN + NORMALIZE_OFFSET, 0.0, 1.0);
    }

    private static ReplacementNoiseFold netherFold() {
        return new ReplacementNoiseFold(NETHER_WEIGHTS, NETHER_HORIZONTAL_SCALE_SLOTS, NETHER_VERTICAL_SCALE_SLOTS);
    }

    private static double foldedNether(ReplacementNoiseFold fold, WorldFold transformer, int quartX, int quartY,
            int quartZ) {
        double[] sum = new double[1];
        GenerationTransformerContext.withTransformer(transformer,
                () -> sum[0] = fold.sum(WORLD_SEED, NETHER_SCALE_QUARTS, quartX, quartY, quartZ,
                        VanillaWorldgen.NETHER_TEMPERATURE));
        return sum[0];
    }

    private static double normalizeNether(double sum) {
        return Mth.clamp(sum / NETHER_SUM_DIVISOR * NORMALIZE_GAIN + NORMALIZE_OFFSET, 0.0, 1.0);
    }

    private static double biolithNetherSum(OpenSimplexNoise2 noise, int quartX, int quartY, int quartZ) {
        double sum = 0.0;
        for (int octave = 0; octave < NETHER_WEIGHTS.length; octave++) {
            double horizontal = NETHER_SCALE_QUARTS[NETHER_HORIZONTAL_SCALE_SLOTS[octave]];
            double vertical = NETHER_SCALE_QUARTS[NETHER_VERTICAL_SCALE_SLOTS[octave]];
            double sample = noise.sample(quartX / horizontal, quartY / vertical, quartZ / horizontal);
            sum += NETHER_WEIGHTS[octave] * sample;
        }

        return sum;
    }

    private static double biolithSum(OpenSimplexNoise2 noise, int quartX, int quartZ) {
        double sum = 0.0;
        for (int octave = 0; octave < OVERWORLD_WEIGHTS.length; octave++) {
            double scale = OVERWORLD_SCALE_QUARTS[octave];
            sum += OVERWORLD_WEIGHTS[octave] * noise.sample(quartX / scale, quartZ / scale);
        }

        return sum;
    }

    private record Spread(double mean, double deviation) {
        static Spread of(double[] values) {
            double total = 0.0;
            for (double value : values) {
                total += value;
            }

            double mean = total / values.length;
            double squares = 0.0;
            for (double value : values) {
                squares += (value - mean) * (value - mean);
            }

            return new Spread(mean, Math.sqrt(squares / values.length));
        }
    }

    private interface Field {
        double at(int quartX, int quartZ);
    }

    private interface Column {
        double at(int quartX, int quartY, int quartZ);
    }

    private interface Line {
        double at(int quartX, int quartY, int quartZ, int offset);
    }

    private static Field foldedField(WorldFold transformer) {
        ReplacementNoiseFold fold = fold();
        return (quartX, quartZ) -> normalize(folded(fold, transformer, quartX, quartZ));
    }

    private static Field biolithField() {
        OpenSimplexNoise2 noise = new OpenSimplexNoise2(WORLD_SEED);
        return (quartX, quartZ) -> normalize(biolithSum(noise, quartX, quartZ));
    }

    private static Field netherField(WorldFold transformer) {
        ReplacementNoiseFold fold = netherFold();
        return (quartX, quartZ) -> normalizeNether(
                foldedNether(fold, transformer, quartX, NETHER_SAMPLE_QUART_Y, quartZ));
    }

    private static Field biolithNetherField() {
        OpenSimplexNoise2 noise = new OpenSimplexNoise2(WORLD_SEED);
        return (quartX, quartZ) -> normalizeNether(
                biolithNetherSum(noise, quartX, NETHER_SAMPLE_QUART_Y, quartZ));
    }

    private static Column netherColumn(WorldFold transformer) {
        ReplacementNoiseFold fold = netherFold();
        return (quartX, quartY, quartZ) -> foldedNether(fold, transformer, quartX, quartY, quartZ);
    }

    private static Spread spread(WorldFold transformer, Field field) {
        double[] values = new double[SAMPLES_PER_AXIS * SAMPLES_PER_AXIS];
        int lapQuarts = transformer.blockDomain(Direction.Axis.X).domainLength / 4;
        int minQuart = transformer.blockDomain(Direction.Axis.X).lowerBound / 4;
        int index = 0;
        for (int xStep = 0; xStep < SAMPLES_PER_AXIS; xStep++) {
            for (int zStep = 0; zStep < SAMPLES_PER_AXIS; zStep++) {
                values[index++] = field.at(minQuart + xStep * lapQuarts / SAMPLES_PER_AXIS,
                        minQuart + zStep * lapQuarts / SAMPLES_PER_AXIS);
            }
        }

        return Spread.of(values);
    }

    private static double belowMedian(WorldFold transformer, Field field) {
        int lapQuarts = transformer.blockDomain(Direction.Axis.X).domainLength / 4;
        int minQuart = transformer.blockDomain(Direction.Axis.X).lowerBound / 4;
        int below = 0;
        for (int xStep = 0; xStep < SAMPLES_PER_AXIS; xStep++) {
            for (int zStep = 0; zStep < SAMPLES_PER_AXIS; zStep++) {
                if (field.at(minQuart + xStep * lapQuarts / SAMPLES_PER_AXIS,
                        minQuart + zStep * lapQuarts / SAMPLES_PER_AXIS) < HALF) {
                    below++;
                }
            }
        }

        return (double) below / (SAMPLES_PER_AXIS * SAMPLES_PER_AXIS);
    }

    private static double crossingsPerQuart(WorldFold transformer, Line line, int samples, int strideQuarts) {
        int lapQuarts = transformer.blockDomain(Direction.Axis.X).domainLength / 4;
        int minQuart = transformer.blockDomain(Direction.Axis.X).lowerBound / 4;
        Random random = new Random(WORLD_SEED);
        long crossed = 0L;
        for (int index = 0; index < REGION_LINES; index++) {
            int quartX = minQuart + random.nextInt(lapQuarts);
            int quartZ = minQuart + random.nextInt(lapQuarts);
            int quartY = random.nextInt(LINE_QUART_Y_BOUND);
            boolean above = line.at(quartX, quartY, quartZ, 0) > 0.0;
            for (int sample = 1; sample < samples; sample++) {
                boolean now = line.at(quartX, quartY, quartZ, sample * strideQuarts) > 0.0;
                if (now != above) {
                    crossed++;
                    above = now;
                }
            }
        }

        return (double) crossed / ((double) REGION_LINES * samples * strideQuarts);
    }

    private static void assertRegionSize(double ours, double biolith) {
        double ratio = ours / biolith;
        assertTrue(Math.abs(ratio - REGION_SIZE_RATIO) <= REGION_TOLERANCE,
                "crossings per quart ours=" + ours + ", biolith=" + biolith + ", ratio=" + ratio);
    }

    private static double[] columnDeviations(WorldFold transformer, Column column) {
        int lapQuarts = transformer.blockDomain(Direction.Axis.X).domainLength / 4;
        int minQuart = transformer.blockDomain(Direction.Axis.X).lowerBound / 4;
        double[] deviations = new double[COLUMNS_PER_AXIS * COLUMNS_PER_AXIS];
        double[] values = new double[NETHER_QUART_HEIGHT];
        int index = 0;
        for (int xStep = 0; xStep < COLUMNS_PER_AXIS; xStep++) {
            for (int zStep = 0; zStep < COLUMNS_PER_AXIS; zStep++) {
                int quartX = minQuart + xStep * lapQuarts / COLUMNS_PER_AXIS;
                int quartZ = minQuart + zStep * lapQuarts / COLUMNS_PER_AXIS;
                for (int quartY = 0; quartY < NETHER_QUART_HEIGHT; quartY++) {
                    values[quartY] = column.at(quartX, quartY, quartZ);
                }

                deviations[index++] = Spread.of(values).deviation();
            }
        }

        return deviations;
    }

    @Nested
    class Periodicity {
        private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(new WorldLoopBounds(-16, 16, -16, 16)));

        @Test
        void repeatsOneLapAway() {
            for (WorldFold transformer : new WorldFold[] {TINY, HUGE, CYLINDER}) {
                ReplacementNoiseFold fold = fold();
                int lapQuarts = transformer.blockDomain(Direction.Axis.X).domainLength / 4;
                Random random = new Random(WORLD_SEED);
                for (int sample = 0; sample < PERIODICITY_SAMPLES; sample++) {
                    int quartX = random.nextInt(-lapQuarts, lapQuarts);
                    int quartZ = random.nextInt(-lapQuarts, lapQuarts);
                    assertEquals(folded(fold, transformer, quartX, quartZ),
                            folded(fold, transformer, quartX + lapQuarts, quartZ),
                            "X lap at quart " + quartX + ", " + quartZ);

                    if (transformer.blockDomain(Direction.Axis.Z).loops()) {
                        assertEquals(folded(fold, transformer, quartX, quartZ),
                                folded(fold, transformer, quartX, quartZ + lapQuarts),
                                "Z lap at quart " + quartX + ", " + quartZ);
                    }
                }
            }
        }

        @Test
        void answersNotFoldedWithoutATransformer() {
            assertFalse(ReplacementNoiseFold.folded(
                    fold().sum(WORLD_SEED, OVERWORLD_SCALE_QUARTS, 0, 0, VanillaWorldgen.OVERWORLD_TEMPERATURE)));
        }

        @Test
        void variesAcrossTheLap() {
            assertTrue(spread(HUGE, foldedField(HUGE)).deviation() > 0.01,
                    "the folded field is flat across a huge world");
        }
    }

    @Nested
    class Distribution {
        @Test
        void spreadMatchesBiolithWhereBothHoldTwoPeriods() {
            Spread ours = spread(HUGE, foldedField(HUGE));
            Spread biolith = spread(HUGE, biolithField());
            String reading = "ours sd=" + ours.deviation() + ", biolith sd=" + biolith.deviation();
            assertTrue(Math.abs(ours.deviation() - biolith.deviation())
                    <= SPREAD_RATIO_TOLERANCE * biolith.deviation(), reading);
        }

        @Test
        void regionsRunAsWideAsBiolithsWhereNoOctaveIsFloored() {
            ReplacementNoiseFold fold = fold();
            OpenSimplexNoise2 noise = new OpenSimplexNoise2(WORLD_SEED);
            double ours = crossingsPerQuart(UNFLOORED, (quartX, quartY, quartZ, offset) ->
                    folded(fold, UNFLOORED, quartX + offset, quartZ),
                    OVERWORLD_LINE_SAMPLES, OVERWORLD_LINE_STRIDE_QUARTS);
            double biolith = crossingsPerQuart(UNFLOORED, (quartX, quartY, quartZ, offset) ->
                    biolithSum(noise, quartX + offset, quartZ),
                    OVERWORLD_LINE_SAMPLES, OVERWORLD_LINE_STRIDE_QUARTS);
            assertRegionSize(ours, biolith);
        }

        @Test
        void regionsShrinkByCompressionFactor() {
            WorldFold strong = compressed(UNFLOORED_CHUNK_MIN, UNFLOORED_CHUNK_MAX, ClimateScale.STRONG);
            WorldFold off = compressed(UNFLOORED_CHUNK_MIN, UNFLOORED_CHUNK_MAX, ClimateScale.OFF);
            ReplacementNoiseFold fold = fold();
            double offCrossings = crossingsPerQuart(off, (quartX, quartY, quartZ, offset) ->
                    folded(fold, off, quartX + offset, quartZ),
                    OVERWORLD_LINE_SAMPLES, 1);
            double strongCrossings = crossingsPerQuart(strong, (quartX, quartY, quartZ, offset) ->
                    folded(fold, strong, quartX + offset, quartZ),
                    OVERWORLD_LINE_SAMPLES, 1);
            double ratio = strongCrossings / offCrossings;
            assertTrue(Math.abs(ratio - ClimateScale.STRONG_FACTOR) <= REGION_TOLERANCE * ClimateScale.STRONG_FACTOR,
                    "crossings per quart off=" + offCrossings + ", strong=" + strongCrossings + ", ratio=" + ratio);
        }

        @Test
        void splitsTheReplacementRangeOnAHugeWorld() {
            assertSplitHolds(HUGE);
        }

        @Test
        void splitsTheReplacementRangeOnATinyWorld() {
            assertSplitHolds(TINY);
        }

        private void assertSplitHolds(WorldFold transformer) {
            double below = belowMedian(transformer, foldedField(transformer));
            assertTrue(Math.abs(below - HALF) <= SPLIT_TOLERANCE,
                    "a rate of one half covers " + below + " of the world");
        }
    }

    @Nested
    class Compression {
        @Test
        void takesTheOverworldTemperatureFactor() {
            assertTakesTheTemperatureFactor(NoiseGeneratorSettings.OVERWORLD, Noises.TEMPERATURE);
        }

        @Test
        void takesTheLargeBiomesTemperatureFactor() {
            assertTakesTheTemperatureFactor(NoiseGeneratorSettings.LARGE_BIOMES, Noises.TEMPERATURE_LARGE);
        }

        @Test
        void takesTheNetherTemperatureFactor() {
            assertTakesTheTemperatureFactor(NoiseGeneratorSettings.NETHER, Noises.TEMPERATURE_NETHER);
        }

        @Test
        void aRouterWithoutAClimateNoiseIsNotCompressed() {
            for (ClimateScale scale : COMPRESSING_SCALES) {
                WorldFold transformer = compressed(TINY_CHUNK_MIN, TINY_CHUNK_MAX, scale);
                assertEquals(NO_COMPRESSION,
                        fold().compression(transformer, VanillaWorldgen.temperatureOf(NoiseGeneratorSettings.END)),
                        FACTOR_TOLERANCE, "the end " + scale.mode().getSerializedName() + " on a tiny world");
            }
        }
    }

    @Nested
    class Nether {
        @Test
        void repeatsOneLapAway() {
            for (WorldFold transformer : new WorldFold[] {NETHER_SCALED, HUGE}) {
                ReplacementNoiseFold fold = netherFold();
                int lapQuarts = transformer.blockDomain(Direction.Axis.X).domainLength / 4;
                Random random = new Random(WORLD_SEED);
                for (int sample = 0; sample < PERIODICITY_SAMPLES; sample++) {
                    int quartX = random.nextInt(-lapQuarts, lapQuarts);
                    int quartZ = random.nextInt(-lapQuarts, lapQuarts);
                    int quartY = random.nextInt(0, NETHER_QUART_HEIGHT);
                    assertEquals(foldedNether(fold, transformer, quartX, quartY, quartZ),
                            foldedNether(fold, transformer, quartX + lapQuarts, quartY, quartZ),
                            "X lap at quart " + quartX + ", " + quartY + ", " + quartZ);
                    assertEquals(foldedNether(fold, transformer, quartX, quartY, quartZ),
                            foldedNether(fold, transformer, quartX, quartY, quartZ + lapQuarts),
                            "Z lap at quart " + quartX + ", " + quartY + ", " + quartZ);
                }
            }
        }

        @Test
        void everyColumnMovesWithY() {
            double[] deviations = columnDeviations(NETHER_SCALED, netherColumn(NETHER_SCALED));
            double flattest = Arrays.stream(deviations).min().orElseThrow();
            assertTrue(flattest > 0.0, "a folded nether column is flat in Y, deviation " + flattest);
        }

        @Test
        void regionsRunAsWideAsBiolithsWhereNoOctaveIsFloored() {
            ReplacementNoiseFold fold = netherFold();
            OpenSimplexNoise2 noise = new OpenSimplexNoise2(WORLD_SEED);
            double ours = crossingsPerQuart(UNFLOORED, (quartX, quartY, quartZ, offset) ->
                    foldedNether(fold, UNFLOORED, quartX + offset, quartY, quartZ),
                    NETHER_LINE_SAMPLES, NETHER_ROW_STRIDE_QUARTS);
            double biolith = crossingsPerQuart(UNFLOORED, (quartX, quartY, quartZ, offset) ->
                    biolithNetherSum(noise, quartX + offset, quartY, quartZ),
                    NETHER_LINE_SAMPLES, NETHER_ROW_STRIDE_QUARTS);
            assertRegionSize(ours, biolith);
        }

        @Test
        void regionsStandAsTallAsBiolithsWhereNoOctaveIsFloored() {
            ReplacementNoiseFold fold = netherFold();
            OpenSimplexNoise2 noise = new OpenSimplexNoise2(WORLD_SEED);
            double ours = crossingsPerQuart(UNFLOORED, (quartX, quartY, quartZ, offset) ->
                    foldedNether(fold, UNFLOORED, quartX, quartY + offset, quartZ),
                    NETHER_LINE_SAMPLES, NETHER_COLUMN_STRIDE_QUARTS);
            double biolith = crossingsPerQuart(UNFLOORED, (quartX, quartY, quartZ, offset) ->
                    biolithNetherSum(noise, quartX, quartY + offset, quartZ),
                    NETHER_LINE_SAMPLES, NETHER_COLUMN_STRIDE_QUARTS);
            assertRegionSize(ours, biolith);
        }

        @Test
        void spreadMatchesBiolithWhereBothHoldPeriods() {
            Spread ours = spread(HUGE, netherField(HUGE));
            Spread biolith = spread(HUGE, biolithNetherField());
            String reading = "ours sd=" + ours.deviation() + ", biolith sd=" + biolith.deviation();
            assertTrue(Math.abs(ours.deviation() - biolith.deviation())
                    <= SPREAD_RATIO_TOLERANCE * biolith.deviation(), reading);
        }

        @Test
        void splitsTheReplacementRangeOnAScaledNether() {
            double below = belowMedian(NETHER_SCALED, netherField(NETHER_SCALED));
            assertTrue(Math.abs(below - HALF) <= SPLIT_TOLERANCE,
                    "a rate of one half covers " + below + " of the nether");
        }
    }
}
