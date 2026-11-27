package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.FlatShape.Identification;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;

import net.minecraft.core.Direction;

class CarriedShapeCodecTest {
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);

    private static final Codec<CarriedShape> CARRIED = CarriedShape.mapCodec(CarriedShape.NO_KEY_PREFIX).codec();

    private static final String FROZEN_ON_DISK_KEY = "wrapping";
    private static final String CLIMATE_SCALE_KEY = CompactBiomes.KEY;
    private static final String GUARANTEED_LAND_KEY = GuaranteedLand.KEY;

    private static final String TORUS_WRAPPING =
            "{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{\"min_chunk\":-16,\"max_chunk\":16}}";
    private static final String TOO_NARROW_WRAPPING =
            "{\"x\":{\"min_chunk\":-4,\"max_chunk\":4},\"z\":{\"min_chunk\":-4,\"max_chunk\":4}}";
    private static final String SKEWED_WRAPPING =
            "{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                    + "\"skew_chunks\":5}";
    private static final String MIRRORED_WRAPPING =
            "{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                    + "\"mirror\":{\"axis\":\"z\",\"line_chunk\":-7}}";

    @Test
    void theShapeFieldCarriesEveryShapeTheEngineCanFold() {
        for (FlatShape shape : List.of(ShapeFixtures.torus(SQUARE), ShapeFixtures.cylinder(X_ONLY),
                FlatShape.latticeTorus(SQUARE, 5))) {
            JsonElement written =
                    CarriedShape.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE, shape).getOrThrow();
            assertEquals(shape, CarriedShape.SHAPE_CODEC.parse(JsonOps.INSTANCE, written).getOrThrow(),
                    written.toString());
        }
    }

    @Test
    void aTorusFieldIsStillTheLegacyWrappingValue() {
        assertEquals(WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, SQUARE).getOrThrow(),
                CarriedShape.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE, ShapeFixtures.torus(SQUARE))
                        .getOrThrow());
    }

    @Test
    void aWorldFileCarryingASkewedLatticeLoads() {
        assertEquals(FlatShape.latticeTorus(SQUARE, 5), CarriedShape.SHAPE_CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(SKEWED_WRAPPING)).getOrThrow());
    }

    @Test
    void aWorldFileCarryingAMirrorRefusesToLoad() {
        assertTrue(readError("{\"x\":{},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                + "\"mirror\":{\"axis\":\"x\",\"line_chunk\":3}}")
                .contains(FlatShape.Identification.MOBIUS.toString()));
    }

    @Test
    void aMirroredWorldFileIsRefusedOnTheLocalIndexFloorRatherThanOnDecomposition() {
        FlatShape klein = ShapeFixtures.mirrored(SQUARE, Direction.Axis.Z, -7);

        assertEquals(
                WorldFolds.verifyFoldable(klein).error().orElseThrow().message(),
                readError("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                        + "\"mirror\":{\"axis\":\"z\",\"line_chunk\":-7}}"));
    }

    @Test
    void aCoupledShapeCannotEvenBeWrittenIntoTheField() {
        DataResult<JsonElement> written = CarriedShape.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE,
                ShapeFixtures.mirrored(SQUARE, Direction.Axis.Z, -7));

        assertTrue(written.isError(), written.toString());
        assertTrue(written.error().orElseThrow().message()
                .contains(FlatShape.Identification.KLEIN.toString()), written.toString());
    }

    @Test
    void theFormerNoiseGeneratorReadsItsShapeThroughTheGate() {
        assertGated(LegacyGenerators.TOROIDAL_CODEC);
    }

    @Test
    void theFormerFlatGeneratorReadsItsShapeThroughTheGate() {
        assertGated(LegacyGenerators.TOROIDAL_FLAT_CODEC);
    }

    private static void assertGated(MapCodec<?> generatorCodec) {
        String mirrored = generatorError(generatorCodec, MIRRORED_WRAPPING);
        assertTrue(mirrored.contains(Identification.KLEIN.toString()), mirrored);

        String narrow = generatorError(generatorCodec, TOO_NARROW_WRAPPING);
        assertTrue(narrow.contains(WorldLoopSizes.describe(WorldLoopSizes.MIN_WIDTH_CHUNKS)), narrow);

        String skewed = generatorError(generatorCodec, SKEWED_WRAPPING);
        assertFalse(skewed.contains(Identification.LATTICE_TORUS.toString()), skewed);

        String torus = generatorError(generatorCodec, TORUS_WRAPPING);
        assertFalse(torus.contains(Identification.KLEIN.toString()), torus);
    }

    private static String generatorError(MapCodec<?> generatorCodec, String wrapping) {
        DataResult<?> result = generatorCodec.codec()
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"wrapping\":" + wrapping + "}"));

        assertTrue(result.isError(), result.toString());
        return result.error().orElseThrow().message();
    }

    @Test
    void aSkewedShapeReadsBack() {
        FlatShape skewed = FlatShape.latticeTorus(SQUARE, 5);

        assertEquals(skewed, roundTrip(carried(skewed, ShapeOptions.DEFAULT)).getShape());
    }

    @Test
    void aShapeCannotBeCarriedMirrored() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> carried(ShapeFixtures.mirrored(SQUARE, Direction.Axis.Z, -7), ShapeOptions.DEFAULT));

        assertTrue(refused.getMessage().contains(Identification.KLEIN.toString()), refused.getMessage());
    }

    @Test
    void aTorusSitsUnderTheWrappingKey() {
        FlatShape torus = ShapeFixtures.torus(SQUARE);
        JsonElement encoded = encoded(carried(torus, ShapeOptions.DEFAULT));

        assertEquals(WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, SQUARE).getOrThrow(),
                encoded.getAsJsonObject().get(FROZEN_ON_DISK_KEY),
                encoded.toString());
        assertEquals(torus, parsed(encoded).getShape());
    }

    @Test
    void aTorusThatCompressesItsClimateWritesTheLayoutOfASaveMadeBeforeTheChoiceExisted() {
        JsonElement encoded = encoded(carried(ShapeFixtures.torus(SQUARE),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.AUTO)));

        assertFalse(encoded.getAsJsonObject().has(CLIMATE_SCALE_KEY), encoded.toString());
        assertEquals(ClimateScale.AUTO, parsed(encoded).getOptions().get(CompactBiomes.FEATURE.getOption()));
    }

    @Test
    void aTorusThatDeclinedCompressionWritesTheChoiceAndReadsItBack() {
        JsonElement encoded = encoded(carried(ShapeFixtures.torus(SQUARE),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.OFF)));

        assertFalse(encoded.getAsJsonObject().get(CLIMATE_SCALE_KEY).getAsBoolean(), encoded.toString());
        assertEquals(ClimateScale.OFF, parsed(encoded).getOptions().get(CompactBiomes.FEATURE.getOption()));
    }

    @Test
    void aWorldFileWithoutTheChoiceLoadsCompressed() {
        JsonElement encoded = encoded(carried(ShapeFixtures.torus(SQUARE),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.OFF)));
        encoded.getAsJsonObject().remove(CLIMATE_SCALE_KEY);

        assertEquals(ClimateScale.AUTO, parsed(encoded).getOptions().get(CompactBiomes.FEATURE.getOption()));
    }

    @Test
    void aTorusThatGuaranteesLandWritesTheChoiceAndReadsItBack() {
        JsonElement encoded = encoded(carried(ShapeFixtures.torus(SQUARE),
                ShapeOptions.DEFAULT.with(GuaranteedLand.FEATURE.getOption(), true)));

        assertTrue(encoded.getAsJsonObject().get(GUARANTEED_LAND_KEY).getAsBoolean(), encoded.toString());
        assertTrue(parsed(encoded).getOptions().get(GuaranteedLand.FEATURE.getOption()));
    }

    @Test
    void aWorldFileWithoutTheLandChoiceLoadsWithoutTheGuarantee() {
        JsonElement encoded = encoded(carried(ShapeFixtures.torus(SQUARE), ShapeOptions.DEFAULT));

        assertFalse(encoded.getAsJsonObject().has(GUARANTEED_LAND_KEY), encoded.toString());
        assertFalse(parsed(encoded).getOptions().get(GuaranteedLand.FEATURE.getOption()));
    }

    private static CarriedShape carried(FlatShape shape, ShapeOptions options) {
        return new CarriedShape(null, shape, options);
    }

    private static JsonElement encoded(CarriedShape carried) {
        return CARRIED.encodeStart(JsonOps.INSTANCE, carried).getOrThrow();
    }

    private static CarriedShape parsed(JsonElement encoded) {
        return CARRIED.parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }

    private static CarriedShape roundTrip(CarriedShape carried) {
        return parsed(encoded(carried));
    }

    private static String readError(String json) {
        DataResult<ShapeModel> result =
                CarriedShape.SHAPE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.isError(), result.toString());
        return result.error().orElseThrow().message();
    }
}
