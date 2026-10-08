package com.exoticworlds.compat.reterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import raccoonman.reterraforged.data.worldgen.preset.settings.ClimateSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Presets;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.climate.ClimateModule;
import raccoonman.reterraforged.world.worldgen.cell.continent.Continent;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.Rivermap;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.util.Seed;

class RtfClimateCompressionTest {
    private static final int SEEDS = 8;

    private static final int LATTICE_SEED = 1000;

    private static final int MODULE_SEED = 7;

    private static final long SAMPLE_SEED = 0x51A7L;

    private static final int LAG_SAMPLES = 20_000;

    private static final double SPREAD = 4096.0;

    private static final double LATTICE_LAG_STEP = 0.01;

    private static final double LATTICE_MAX_LAG = 3.0;

    private static final double CELL_LAG_STEP = 0.05;

    private static final double CELL_MAX_LAG = 40.0;

    private static final double PLANE_STRIDE = 0.618;

    private static final double PLANE_SPAN = 64.0;

    private static final double HALF = 0.5;

    private static final double RATE_TOLERANCE = 0.03;

    private static final double SHRINK_TOLERANCE = 0.1;

    private static final double EXACT = 1.0E-9;

    private static final int WORLD_HEIGHT = 384;

    private static final int SEA_LEVEL = 63;

    private static final float LAND_VALUE = 0.8F;

    private static final float CONTINENT_SLACK_BLOCKS = 450.0F;

    private static final int NARROW_CHUNKS = 64;

    private static final int WIDE_CHUNKS = 5000;

    private static final int CUSTOM_FACTOR = 4;

    private static final double COMPRESSION = 4.0;

    private static final double FRACTIONAL_COMPRESSION = 2.5;

    private static final int CLIMATE_LINES = 4;

    private static final int CLIMATE_STEPS = 16384;

    private static final int CLIMATE_MAX_LAG = 1024;

    private static final float CLIMATE_STEP_BLOCKS = 4.0F;

    private static final int STRIP = 16;

    private static final float STEP_FACTOR = 4.0F;

    private static final float STEP_SLACK = 1.0E-5F;

    private static final int CROSS_LINES = 64;

    private static final RtfClimateScales DEFAULT_SCALES = scales(defaultModule(MODULE_SEED));

    private interface Sampled {
        double at(double x, double z);
    }

    private record Scales(float biomeFrequency, int temperatureScale, int moistureScale) implements RtfClimateScales {
        @Override
        public float toroidal$biomeFrequency() {
            return this.biomeFrequency;
        }

        @Override
        public int toroidal$temperatureScale() {
            return this.temperatureScale;
        }

        @Override
        public int toroidal$moistureScale() {
            return this.moistureScale;
        }
    }

    private static final class AskedContinent implements Continent {
        private float lowestX = Float.POSITIVE_INFINITY;
        private float highestX = Float.NEGATIVE_INFINITY;

        @Override
        public float getEdgeValue(float x, float z) {
            this.lowestX = Math.min(this.lowestX, x);
            this.highestX = Math.max(this.highestX, x);
            return LAND_VALUE;
        }

        @Override
        public long getNearestCenter(float x, float z) {
            return 0L;
        }

        @Override
        public Rivermap getRivermap(int x, int z) {
            return null;
        }

        @Override
        public void apply(Cell cell, float x, float z) {
        }
    }

    @BeforeAll
    static void activate() {
        RtfLap.activate();
    }

    @Test
    void theRatesAreMeasuredOnReTerraForgedsOwnClimate() throws ReflectiveOperationException {
        double lattice = 0.0;
        double temperature = 0.0;
        double moistureX = 0.0;
        double moistureZ = 0.0;
        for (int seed = 0; seed < SEEDS; seed++) {
            ImprovedNoise noise = new ImprovedNoise(RandomSource.create(LATTICE_SEED + seed));
            lattice += halfLag((x, z) -> noise.noise(x, (Math.floor(z) * PLANE_STRIDE) % PLANE_SPAN, z), true,
                    LATTICE_LAG_STEP, LATTICE_MAX_LAG);
            ClimateModule module = defaultModule(MODULE_SEED + seed);
            RtfClimateScales scales = scales(module);
            Noise temperatureNoise = noise(module, "temperature");
            Noise moistureNoise = noise(module, "moisture");
            temperature += halfLag((x, z) -> temperatureNoise.compute((float) x, (float) z, 0), false,
                    CELL_LAG_STEP, CELL_MAX_LAG) / scales.toroidal$temperatureScale();
            moistureX += halfLag((x, z) -> moistureNoise.compute((float) x, (float) z, 0), true,
                    CELL_LAG_STEP, CELL_MAX_LAG) / scales.toroidal$moistureScale();
            moistureZ += halfLag((x, z) -> moistureNoise.compute((float) x, (float) z, 0), false,
                    CELL_LAG_STEP, CELL_MAX_LAG) / scales.toroidal$moistureScale();
        }

        String measured = "lattice " + lattice / SEEDS + ", temperature z " + temperature / SEEDS + ", moisture x "
                + moistureX / SEEDS + ", moisture z " + moistureZ / SEEDS;
        assertRate(RtfClimateCompression.LATTICE_HALF_CELLS, lattice / SEEDS, measured);
        assertRate(RtfClimateCompression.TEMPERATURE_HALF_Z, temperature / SEEDS, measured);
        assertRate(RtfClimateCompression.MOISTURE_HALF_X, moistureX / SEEDS, measured);
        assertRate(RtfClimateCompression.MOISTURE_HALF_Z, moistureZ / SEEDS, measured);
    }

    @Test
    void theScalesAreReadFromTheModule() {
        ClimateSettings settings = Presets.makeRTFDefault().climate();
        RtfClimateScales scales = DEFAULT_SCALES;
        assertEquals(1.0F / settings.biomeShape.biomeSize, scales.toroidal$biomeFrequency());
        assertTrue(scales.toroidal$temperatureScale() > 0, "the temperature scale was kept");
        assertTrue(scales.toroidal$moistureScale() > scales.toroidal$temperatureScale(),
                "the moisture scale, 2.5 times the temperature's in the preset, was kept apart from it");
    }

    @Test
    void offAndCustomGiveTheirOwnFactor() {
        for (WorldFold fold : shapes(NARROW_CHUNKS, ClimateScale.OFF)) {
            assertEquals(ClimateScaleCompression.NO_COMPRESSION, RtfClimateCompression.factor(fold, DEFAULT_SCALES));
        }

        for (WorldFold fold : shapes(NARROW_CHUNKS, ClimateScale.custom(CUSTOM_FACTOR))) {
            assertEquals(CUSTOM_FACTOR, RtfClimateCompression.factor(fold, DEFAULT_SCALES));
        }
    }

    @Test
    void autoFitsTheClimateIntoTheLapAndLeavesAWideWorldAlone() {
        WorldFold[] narrow = shapes(NARROW_CHUNKS, ClimateScale.AUTO);
        double torus = RtfClimateCompression.factor(narrow[0], DEFAULT_SCALES);
        double alongX = RtfClimateCompression.factor(narrow[1], DEFAULT_SCALES);
        double alongZ = RtfClimateCompression.factor(narrow[2], DEFAULT_SCALES);
        assertTrue(alongX > ClimateScaleCompression.NO_COMPRESSION && alongZ > ClimateScaleCompression.NO_COMPRESSION,
                "a 1024-block lap is compressed: " + alongX + " along X, " + alongZ + " along Z");
        assertEquals(Math.max(alongX, alongZ), torus, EXACT);
        assertEquals(ClimateScaleCompression.CELLS_PER_LAP, lapBlocks(narrow[1], Direction.Axis.X) * alongX
                * RtfClimateCompression.LATTICE_HALF_CELLS
                / RtfClimateCompression.halfBlocks(Direction.Axis.X, DEFAULT_SCALES), EXACT);

        for (WorldFold fold : shapes(WIDE_CHUNKS, ClimateScale.AUTO)) {
            assertEquals(ClimateScaleCompression.NO_COMPRESSION, RtfClimateCompression.factor(fold, DEFAULT_SCALES));
        }
    }

    @Test
    void theTemperatureDecidesAlongZAlone() {
        WorldFold[] narrow = shapes(NARROW_CHUNKS, ClimateScale.AUTO);
        RtfClimateScales wideTemperature = new Scales(DEFAULT_SCALES.toroidal$biomeFrequency(),
                2 * DEFAULT_SCALES.toroidal$temperatureScale(), DEFAULT_SCALES.toroidal$moistureScale());
        assertEquals(RtfClimateCompression.factor(narrow[1], DEFAULT_SCALES),
                RtfClimateCompression.factor(narrow[1], wideTemperature), EXACT);
        assertTrue(RtfClimateCompression.factor(narrow[2], wideTemperature)
                > RtfClimateCompression.factor(narrow[2], DEFAULT_SCALES));
    }

    @Test
    void strongTakesTheFitOrFourWhicheverIsLarger() {
        WorldFold[] auto = shapes(NARROW_CHUNKS, ClimateScale.AUTO);
        WorldFold[] strong = shapes(NARROW_CHUNKS, ClimateScale.STRONG);
        for (int i = 0; i < auto.length; i++) {
            assertEquals(Math.max(ClimateScale.STRONG_FACTOR, RtfClimateCompression.factor(auto[i], DEFAULT_SCALES)),
                    RtfClimateCompression.factor(strong[i], DEFAULT_SCALES), EXACT);
        }
    }

    @Test
    void aFactorOfOneLeavesTheClimateAsItWas() {
        ClimateModule module = defaultModule(MODULE_SEED);
        WorldFold fold = shapes(NARROW_CHUNKS, ClimateScale.OFF)[0];
        RtfLap.Frame frame = RtfLap.frame();
        Random random = new Random(SAMPLE_SEED);
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            for (int i = 0; i < LAG_SAMPLES; i++) {
                float x = frame.shift(Direction.Axis.X, (float) (random.nextDouble() * SPREAD));
                float z = frame.shift(Direction.Axis.Z, (float) (random.nextDouble() * SPREAD));
                Cell plain = climate(module, x, z);
                Cell compressed;
                try (RtfLap.Frame.Scope once = frame.compress(ClimateScaleCompression.NO_COMPRESSION)) {
                    compressed = climate(module, x, z);
                }

                assertEquals(plain.regionTemperature, compressed.regionTemperature);
                assertEquals(plain.regionMoisture, compressed.regionMoisture);
                assertEquals(plain.biomeRegionId, compressed.biomeRegionId);
                assertEquals(plain.macroBiomeId, compressed.macroBiomeId);
            }
        }
    }

    @Test
    void compressionShrinksTheClimateAndAsksTheContinentAboutTheWorld() {
        AskedContinent continent = new AskedContinent();
        ClimateModule module = new ClimateModule(new Seed(MODULE_SEED), continent, null,
                Presets.makeRTFDefault().climate(), new Levels(WORLD_HEIGHT, SEA_LEVEL));
        WorldFold fold = shapes(WIDE_CHUNKS, ClimateScale.OFF)[0];
        RtfLap.Frame frame = RtfLap.frame();
        double plain;
        double compressed;
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            plain = temperatureHalfAlongZ(module, frame, ClimateScaleCompression.NO_COMPRESSION);
            compressed = temperatureHalfAlongZ(module, frame, COMPRESSION);
        }

        assertEquals(COMPRESSION, plain / compressed, COMPRESSION * SHRINK_TOLERANCE,
                "temperature half correlation " + plain + " blocks plain, " + compressed + " compressed");
        assertTrue(continent.lowestX >= -CONTINENT_SLACK_BLOCKS
                        && continent.highestX <= SPREAD + CONTINENT_SLACK_BLOCKS,
                "the continent was asked between " + continent.lowestX + " and " + continent.highestX
                        + ", the climate was read between 0 and " + SPREAD);
    }

    // Positions enter the climate moved onto [0, lap), so its lattice closes where a canonical coordinate passes zero.
    @Test
    void theCompressedBiomeCellsJoinWhereTheLatticeCloses() {
        ClimateModule module = defaultModule(MODULE_SEED);
        for (WorldFold fold : shapes(NARROW_CHUNKS, ClimateScale.OFF)) {
            RtfLap.Frame frame = RtfLap.frame();
            try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
                for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                    if (fold.blockDomain(axis).loops()) {
                        for (double factor : new double[] {COMPRESSION, FRACTIONAL_COMPRESSION}) {
                            assertJoinedAt(module, frame, fold, axis, 0, factor);
                            assertJoinedAt(module, frame, fold, axis, fold.blockDomain(axis).lowerBound
                                    + fold.blockDomain(axis).domainLength, factor);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theFrameIsLeftAsItWasFound() {
        RtfLap.Frame frame = RtfLap.frame();
        WorldFold fold = shapes(NARROW_CHUNKS, ClimateScale.OFF)[0];
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            try (RtfLap.Frame.Scope compressed = frame.compress(COMPRESSION);
                    RtfLap.Frame.Scope expanded = frame.expand()) {
                assertEquals(lapBlocks(fold, Direction.Axis.X), frame.lap(Direction.Axis.X), EXACT);
                assertEquals(ClimateScaleCompression.NO_COMPRESSION, frame.compression());
            }

            assertEquals(lapBlocks(fold, Direction.Axis.X), frame.lap(Direction.Axis.X), EXACT);
            assertEquals(ClimateScaleCompression.NO_COMPRESSION, frame.compression());
        }
    }

    @Test
    void theJoinBreaksWhereAFractionalFactorLeavesTheLapUncompressed() {
        ClimateModule module = defaultModule(MODULE_SEED);
        WorldFold fold = shapes(NARROW_CHUNKS, ClimateScale.OFF)[0];
        int span = fold.blockDomain(Direction.Axis.Z).domainLength;
        int crossMin = fold.blockDomain(Direction.Axis.Z).lowerBound;
        RtfLap.Frame frame = RtfLap.frame();
        float[] edge = new float[2 * STRIP + 1];
        int jumps = 0;
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            for (int line = 0; line < CROSS_LINES; line++) {
                float cross = frame.shift(Direction.Axis.Z, crossMin + line * span / (float) CROSS_LINES);
                for (int i = 0; i <= 2 * STRIP; i++) {
                    float along = frame.shift(Direction.Axis.X, i - STRIP);
                    edge[i] = climate(module, (float) (along * FRACTIONAL_COMPRESSION),
                            (float) (cross * FRACTIONAL_COMPRESSION)).biomeRegionEdge;
                }

                if (!stepJoined(edge)) {
                    jumps++;
                }
            }
        }

        assertTrue(jumps > CROSS_LINES / 2, "the uncompressed lap jumped on " + jumps + " of " + CROSS_LINES
                + " lines");
    }

    private static void assertJoinedAt(ClimateModule module, RtfLap.Frame frame, WorldFold fold,
            Direction.Axis axis, int join, double factor) {
        Direction.Axis crossAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        int span = fold.blockDomain(axis).domainLength;
        boolean crossLoops = fold.blockDomain(crossAxis).loops();
        int crossMin = crossLoops ? fold.blockDomain(crossAxis).lowerBound : -span / 2;
        int crossSpan = crossLoops ? fold.blockDomain(crossAxis).domainLength : span;
        float[] edge = new float[2 * STRIP + 1];
        for (int line = 0; line < CROSS_LINES; line++) {
            int cross = crossMin + line * crossSpan / CROSS_LINES;
            for (int i = 0; i <= 2 * STRIP; i++) {
                float along = join - STRIP + i;
                Cell cell = axis == Direction.Axis.X
                        ? compressedClimate(module, frame, along, cross, factor)
                        : compressedClimate(module, frame, cross, along, factor);
                edge[i] = cell.biomeRegionEdge;
            }

            assertTrue(stepJoined(edge), "the biome cell edge jumps across " + join + " on " + axis + " at "
                    + cross + " with factor " + factor);
        }
    }

    private static boolean stepJoined(float[] values) {
        float seamStep = Math.abs(values[STRIP] - values[STRIP - 1]);
        float besideStep = 0.0F;
        for (int i = 0; i < 2 * STRIP; i++) {
            if (i != STRIP - 1) {
                besideStep = Math.max(besideStep, Math.abs(values[i + 1] - values[i]));
            }
        }

        return seamStep <= STEP_FACTOR * besideStep + STEP_SLACK;
    }

    private static double temperatureHalfAlongZ(ClimateModule module, RtfLap.Frame frame, double factor) {
        int lines = CLIMATE_LINES;
        double sum = 0.0;
        float[][] values = new float[lines][CLIMATE_STEPS + CLIMATE_MAX_LAG];
        for (int line = 0; line < lines; line++) {
            float x = line * (float) SPREAD / lines;
            for (int step = 0; step < values[line].length; step++) {
                values[line][step] = compressedClimate(module, frame, x, step * CLIMATE_STEP_BLOCKS, factor)
                        .regionTemperature;
            }
        }

        for (float[] line : values) {
            for (int i = 0; i < CLIMATE_STEPS; i++) {
                sum += line[i];
            }
        }

        double mean = sum / (lines * CLIMATE_STEPS);
        double[] products = new double[CLIMATE_MAX_LAG + 1];
        for (float[] line : values) {
            for (int i = 0; i < CLIMATE_STEPS; i++) {
                for (int lag = 0; lag <= CLIMATE_MAX_LAG; lag++) {
                    products[lag] += (line[i] - mean) * (line[i + lag] - mean);
                }
            }
        }

        double before = 1.0;
        for (int lag = 1; lag <= CLIMATE_MAX_LAG; lag++) {
            double correlation = products[lag] / products[0];
            if (correlation <= HALF) {
                return (lag - 1 + (before - HALF) / (before - correlation)) * CLIMATE_STEP_BLOCKS;
            }

            before = correlation;
        }

        throw new AssertionError("the temperature never fell to half its correlation at factor " + factor);
    }

    private static Cell compressedClimate(ClimateModule module, RtfLap.Frame frame, float x, float z,
            double factor) {
        float shiftedX = frame.shift(Direction.Axis.X, x);
        float shiftedZ = frame.shift(Direction.Axis.Z, z);
        if (factor == ClimateScaleCompression.NO_COMPRESSION) {
            return climate(module, shiftedX, shiftedZ);
        }

        try (RtfLap.Frame.Scope compressed = frame.compress(factor)) {
            return climate(module, (float) (shiftedX * factor), (float) (shiftedZ * factor));
        }
    }

    private static Cell climate(ClimateModule module, float x, float z) {
        Cell cell = new Cell();
        module.apply(cell, x, z, x, z, true);
        return cell;
    }

    private static WorldFold[] shapes(int chunks, ClimateScale scale) {
        GenerationOptions options = GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, scale);
        return new WorldFold[] {
                WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunks)), options),
                WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, chunks)), options),
                WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.Z, chunks)), options)};
    }

    private static double lapBlocks(WorldFold fold, Direction.Axis axis) {
        return fold.blockDomain(axis).domainLength;
    }

    private static ClimateModule defaultModule(int seed) {
        return new ClimateModule(new Seed(seed), new AskedContinent(), null, Presets.makeRTFDefault().climate(),
                new Levels(WORLD_HEIGHT, SEA_LEVEL));
    }

    private static RtfClimateScales scales(ClimateModule module) {
        return (RtfClimateScales) (Object) module;
    }

    private static Noise noise(ClimateModule module, String name) throws ReflectiveOperationException {
        Field field = ClimateModule.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Noise) field.get(module);
    }

    private static void assertRate(double pinned, double measured, String readings) {
        assertEquals(pinned, measured, pinned * RATE_TOLERANCE, readings);
    }

    private static double halfLag(Sampled field, boolean alongX, double step, double maxLag) {
        Random random = new Random(SAMPLE_SEED);
        double[] xs = new double[LAG_SAMPLES];
        double[] zs = new double[LAG_SAMPLES];
        double[] base = new double[LAG_SAMPLES];
        double sum = 0.0;
        for (int i = 0; i < LAG_SAMPLES; i++) {
            xs[i] = random.nextDouble() * SPREAD;
            zs[i] = random.nextDouble() * SPREAD;
            base[i] = field.at(xs[i], zs[i]);
            sum += base[i];
        }

        double mean = sum / LAG_SAMPLES;
        double variance = 0.0;
        for (double value : base) {
            variance += (value - mean) * (value - mean);
        }

        double previousLag = 0.0;
        double previousCorrelation = 1.0;
        for (double lag = step; lag <= maxLag; lag += step) {
            double covariance = 0.0;
            for (int i = 0; i < LAG_SAMPLES; i++) {
                double x = alongX ? xs[i] + lag : xs[i];
                double z = alongX ? zs[i] : zs[i] + lag;
                covariance += (base[i] - mean) * (field.at(x, z) - mean);
            }

            double correlation = covariance / variance;
            if (correlation <= HALF) {
                return previousLag + (lag - previousLag) * (previousCorrelation - HALF)
                        / (previousCorrelation - correlation);
            }

            previousLag = lag;
            previousCorrelation = correlation;
        }

        throw new AssertionError("the correlation never fell to one half within " + maxLag + " cells");
    }
}
