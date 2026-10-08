package com.exoticworlds.shape.climate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;

class ClimateCompressionTest {
    private static final double XZ_SCALE = 0.25;

    private static final double HORIZONTAL = 0.0;

    private static final double TOLERANCE = 1.0E-6;

    private static final boolean CLIMATE = true;
    private static final boolean COAST = false;

    private static final String MODE_KEY = "mode";
    private static final String FACTOR_KEY = "factor";

    private static final int[] PRESET_CHUNK_WIDTHS = {32, 64, 128, 256, 512};

    private record Octave(double amplitude, double cellBlocks) {
    }

    private record Field(String name, int firstOctave, DoubleList amplitudes, List<Octave> octaves,
            boolean climate) {
    }

    private static final Field TEMPERATURE = new Field("temperature", -10,
            DoubleArrayList.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.5, 4096.0), new Octave(1.0, 1024.0)), CLIMATE);

    private static final Field TEMPERATURE_LARGE = new Field("temperature_large", -12,
            DoubleArrayList.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.5, 16384.0), new Octave(1.0, 4096.0)), CLIMATE);

    private static final Field VEGETATION = new Field("vegetation", -8,
            DoubleArrayList.of(1.0, 1.0, 0.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.0, 1024.0), new Octave(1.0, 512.0)), CLIMATE);

    private static final Field CONTINENTALNESS = new Field("continentalness", -9,
            DoubleArrayList.of(1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0),
            List.of(new Octave(1.0, 2048.0), new Octave(1.0, 1024.0), new Octave(2.0, 512.0),
                    new Octave(2.0, 256.0), new Octave(2.0, 128.0), new Octave(1.0, 64.0),
                    new Octave(1.0, 32.0), new Octave(1.0, 16.0), new Octave(1.0, 8.0)), COAST);

    private static final Field EROSION = new Field("erosion", -9,
            DoubleArrayList.of(1.0, 1.0, 0.0, 1.0, 1.0),
            List.of(new Octave(1.0, 2048.0), new Octave(1.0, 1024.0), new Octave(1.0, 256.0),
                    new Octave(1.0, 128.0)), COAST);

    private static final Field TEMPERATURE_NETHER = new Field("temperature_nether", -7,
            DoubleArrayList.of(1.0, 1.0),
            List.of(new Octave(1.0, 512.0), new Octave(1.0, 256.0)), CLIMATE);

    private static double expected(Field field, double lapBlocks) {
        double weighted = 0.0;
        double weight = 0.0;

        for (Octave octave : field.octaves()) {
            double square = octave.amplitude() * octave.amplitude();
            weighted += square * (lapBlocks / octave.cellBlocks());
            weight += square;
        }

        double cellsPerLap = weighted / weight;
        return Math.max(1.0, ClimateScaleCompression.CELLS_PER_LAP / cellsPerLap);
    }

    private static double actual(Field field, WorldFold fold, double verticalShare) {
        return ClimateCompression.factor(fold, field.climate(), field.amplitudes(),
                Math.pow(2.0, field.firstOctave()), XZ_SCALE, verticalShare);
    }

    private static WorldFold square(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)));
    }

    private static WorldFold uncompressedSquare(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF));
    }

    private static WorldFold strongSquare(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.STRONG));
    }

    private static WorldFold customSquare(int chunkWidth, int factor) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.custom(factor)));
    }

    private static void assertFactor(Field field, int chunkWidth) {
        double lapBlocks = chunkWidth * 16.0;
        assertEquals(expected(field, lapBlocks), actual(field, square(chunkWidth), HORIZONTAL), TOLERANCE,
                field.name() + " on " + (int) lapBlocks + " blocks");
    }

    @Test
    void everyClimateFieldMatchesTheCellSizeArithmeticOnEveryPreset() {
        for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION, CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : new int[] {32, 64, 128, 256, 512}) {
                assertFactor(field, chunkWidth);
            }
        }
    }

    @Test
    void aTorusThatDeclinedCompressionIsMultipliedByExactlyOneOnEveryFieldAndPreset() {
        for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION, CONTINENTALNESS, EROSION,
                TEMPERATURE_NETHER)) {
            for (int chunkWidth : new int[] {16, 32, 64, 128, 256, 512}) {
                assertEquals(1.0, actual(field, uncompressedSquare(chunkWidth), HORIZONTAL), 0.0,
                        field.name() + " uncompressed on " + chunkWidth * 16 + " blocks");
            }
        }
    }

    @Test
    void theFieldsThatShapeTheCoastAreLeftAloneOnTheNarrowestWorld() {
        assertEquals(1.0, actual(CONTINENTALNESS, square(32), HORIZONTAL), TOLERANCE, "continentalness tiny");
        assertEquals(1.0, actual(EROSION, square(32), HORIZONTAL), TOLERANCE, "erosion tiny");
    }

    @Test
    void temperatureIsUntouchedOnTheWidestPreset() {
        assertEquals(1.0, actual(TEMPERATURE, square(512), HORIZONTAL), TOLERANCE, "temperature huge");
        assertEquals(1.0, actual(TEMPERATURE, square(1024), HORIZONTAL), TOLERANCE, "16384 blocks by hand");
    }

    @Test
    void aFieldAtOrAboveTheTargetIsMultipliedByExactlyOne() {
        assertEquals(1.0, actual(TEMPERATURE, square(512), HORIZONTAL), 0.0, "temperature huge");
        assertEquals(1.0, actual(TEMPERATURE, square(1024), HORIZONTAL), 0.0, "16384 blocks by hand");
        assertEquals(1.0, actual(CONTINENTALNESS, square(32), HORIZONTAL), 0.0, "continentalness tiny");
        assertEquals(1.0, actual(EROSION, square(32), HORIZONTAL), 0.0, "erosion tiny");
    }

    @Test
    void theNetherIsMeasuredOnItsOwnNarrowerWidth() {
        assertEquals(expected(TEMPERATURE_NETHER, 256.0), actual(TEMPERATURE_NETHER, square(16), HORIZONTAL),
                TOLERANCE, "256-block nether under the three smallest presets");
        assertEquals(1.0, actual(TEMPERATURE_NETHER, square(64), HORIZONTAL), 0.0, "1024-block nether under huge");
    }

    @Test
    void aVerticallyLiveFieldIsNeverCompressed() {
        assertEquals(1.0, actual(TEMPERATURE, square(32), 0.5), TOLERANCE, "declared vertical share");
        assertEquals(1.0, actual(TEMPERATURE, square(32), GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE),
                TOLERANCE, "undeclared vertical share");
    }

    @Test
    void theShorterLapDrivesARectangularWorld() {
        WorldFold rectangular = WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(-16, 16, -8, 8)));

        assertEquals(expected(TEMPERATURE, 256.0), actual(TEMPERATURE, rectangular, HORIZONTAL), TOLERANCE,
                "256 blocks on Z against 512 on X");
    }

    @Test
    void aCylinderIsCompressedAsATorusOfTheSameLapInEveryMode() {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                for (ClimateScale scale : List.of(ClimateScale.OFF, ClimateScale.AUTO, ClimateScale.STRONG,
                        ClimateScale.custom(6))) {
                    GenerationOptions options = GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, scale);
                    WorldFold cylinder = WorldFolds.of(
                            FlatShape.cylinder(WorldLoopBounds.ofWidth(axis, chunkWidth)), options);
                    WorldFold torus = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)), options);
                    String where = axis + " cylinder of " + chunkWidth * 16 + " blocks under " + scale.mode();

                    for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION, CONTINENTALNESS)) {
                        assertEquals(actual(field, torus, HORIZONTAL), actual(field, cylinder, HORIZONTAL), 0.0,
                                field.name() + " on a " + where);
                    }
                }
            }
        }
    }

    @Test
    void aCylinderThatDeclinedCompressionIsMultipliedByExactlyOne() {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(axis, 32)),
                    GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF));

            assertEquals(1.0, actual(TEMPERATURE, cylinder, HORIZONTAL), 0.0, "temperature on an " + axis + " cylinder");
            assertEquals(1.0, actual(VEGETATION, cylinder, HORIZONTAL), 0.0, "vegetation on an " + axis + " cylinder");
        }
    }

    @Test
    void strongIsAutoOrFourTimes_whicheverIsLarger_onEveryClimateFieldAndPreset() {
        for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                double auto = actual(field, square(chunkWidth), HORIZONTAL);
                double strong = actual(field, strongSquare(chunkWidth), HORIZONTAL);
                String where = field.name() + " on " + chunkWidth * 16 + " blocks";

                assertEquals(Math.max(auto, ClimateScale.STRONG_FACTOR), strong, TOLERANCE, where);
                assertTrue(strong >= auto, where + ": strong is weaker than auto");
            }
        }
    }

    @Test
    void strongShrinksTheWidestPresetsWhereAutoDoesNothing() {
        for (int chunkWidth : new int[] {256, 512}) {
            String where = "temperature on " + chunkWidth * 16 + " blocks";

            assertEquals(1.0, actual(TEMPERATURE, square(chunkWidth), HORIZONTAL), 0.0, where + ", auto");
            assertEquals(ClimateScale.STRONG_FACTOR, actual(TEMPERATURE, strongSquare(chunkWidth), HORIZONTAL),
                    0.0, where + ", strong");
        }
    }

    @Test
    void strongLeavesTheFieldsThatShapeTheCoastAloneOnEveryPreset() {
        for (Field field : List.of(CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                assertEquals(1.0, actual(field, strongSquare(chunkWidth), HORIZONTAL), 0.0,
                        field.name() + " under strong on " + chunkWidth * 16 + " blocks");
            }
        }
    }

    @Test
    void customAppliesTheTypedFactorExactlyAndIsNeverCombinedWithAuto() {
        for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
            assertEquals(6.0, actual(TEMPERATURE, customSquare(chunkWidth, 6), HORIZONTAL), 0.0,
                    "temperature under custom 6 on " + chunkWidth * 16 + " blocks");
        }

        assertEquals(2.0, actual(TEMPERATURE, customSquare(32, 2), HORIZONTAL), 0.0,
                "custom 2 on a tiny world, where auto asks for more");
    }

    @Test
    void customLeavesTheFieldsThatShapeTheCoastAloneOnEveryPreset() {
        for (Field field : List.of(CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                assertEquals(1.0, actual(field, customSquare(chunkWidth, 16), HORIZONTAL), 0.0,
                        field.name() + " under custom 16 on " + chunkWidth * 16 + " blocks");
            }
        }
    }

    @Test
    void aFixedModeIsRefusedByAVerticallyLiveField() {
        assertEquals(1.0, actual(TEMPERATURE, strongSquare(32), 0.5), 0.0, "strong, declared vertical share");
        assertEquals(1.0, actual(TEMPERATURE, customSquare(32, 8), 0.5), 0.0, "custom, declared vertical share");
    }

    @Test
    void customReachesWhatAutoFitsOnTheSmallestLargeBiomesWorld() {
        double auto = actual(TEMPERATURE_LARGE, square(32), HORIZONTAL);

        assertTrue(auto <= ClimateScale.CUSTOM_MAX, "auto fits " + auto + ", above the custom ceiling");
        assertEquals(ClimateScale.CUSTOM_MAX, actual(TEMPERATURE_LARGE, customSquare(32, ClimateScale.CUSTOM_MAX),
                HORIZONTAL), 0.0, "temperature_large under custom at the ceiling on 512 blocks");
    }

    @Test
    void theCodecTakesTheCeilingRefusesPastItAndReadsAnOlderWorldUnchanged() {
        assertEquals(ClimateScale.custom(ClimateScale.CUSTOM_MAX), decode(customJson(ClimateScale.CUSTOM_MAX)));
        assertTrue(ClimateScale.CODEC.parse(JsonOps.INSTANCE, customJson(ClimateScale.CUSTOM_MAX + 1)).isError(),
                "a factor past the ceiling parses");
        assertEquals(ClimateScale.custom(16), decode(customJson(16)));
        assertEquals(ClimateScale.AUTO, decode(new JsonPrimitive(true)));
        assertEquals(ClimateScale.OFF, decode(new JsonPrimitive(false)));
    }

    private static JsonElement customJson(int factor) {
        JsonObject custom = new JsonObject();
        custom.addProperty(MODE_KEY, ClimateScale.Mode.CUSTOM.getSerializedName());
        custom.addProperty(FACTOR_KEY, factor);
        return custom;
    }

    private static ClimateScale decode(JsonElement json) {
        return ClimateScale.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(message -> new AssertionError(message));
    }

    @Test
    void offIsExactlyOneInEveryModeAwareBranch() {
        for (Field field : List.of(TEMPERATURE, VEGETATION, CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                assertEquals(1.0, actual(field, uncompressedSquare(chunkWidth), HORIZONTAL), 0.0,
                        field.name() + " off on " + chunkWidth * 16 + " blocks");
            }
        }
    }
}
