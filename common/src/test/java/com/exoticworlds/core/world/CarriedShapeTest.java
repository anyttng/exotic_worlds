package com.exoticworlds.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

class CarriedShapeTest {
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds NARROWER = WorldLoopBounds.ofWidth(16);
    private static final WorldLoopBounds TOO_NARROW = WorldLoopBounds.ofWidth(WorldLoopSizes.MIN_WIDTH_CHUNKS - 2);

    private static final String NO_PREFIX = "";
    private static final String STAMP_PREFIX = ExoticWorlds.MODID + ":";

    private static final List<String> PREFIXES = List.of(NO_PREFIX, STAMP_PREFIX);

    @Test
    void everyPrefixCarriesTheShapeAndTheChosenOptionsBackUnchanged() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.OFF));

        for (String prefix : PREFIXES) {
            MapCodec<CarriedShape> codec = CarriedShape.mapCodec(prefix);
            JsonElement written = codec.codec().encodeStart(JsonOps.INSTANCE, carried).getOrThrow();

            assertTrue(written.getAsJsonObject().has(prefix + CarriedShape.WRAPPING_KEY), written.toString());
            assertTrue(written.getAsJsonObject().has(prefix + CompactBiomes.KEY), written.toString());
            assertEquals(carried, codec.codec().parse(JsonOps.INSTANCE, written).getOrThrow(), written.toString());
        }
    }

    @Test
    void anOptionLeftAtItsDefaultIsNeverWrittenAndComesBackAsTheDefault() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE), ShapeOptions.DEFAULT);

        for (String prefix : PREFIXES) {
            MapCodec<CarriedShape> codec = CarriedShape.mapCodec(prefix);
            JsonElement written = codec.codec().encodeStart(JsonOps.INSTANCE, carried).getOrThrow();

            assertFalse(written.getAsJsonObject().has(prefix + CompactBiomes.KEY), written.toString());
            assertEquals(carried, codec.codec().parse(JsonOps.INSTANCE, written).getOrThrow(), written.toString());
        }
    }

    @Test
    void theFoldIsBuiltOnceAndHandedBackAsTheSameReference() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE), ShapeOptions.DEFAULT);

        assertSame(carried.getFold(), carried.getFold());
        assertTrue(carried.getFold().isWrapped());
    }

    @Test
    void aDerivedShapeKeepsTheOptionsItWasCarriedWith() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.OFF));

        CarriedShape derived = carried.withShape(ShapeFixtures.torus(NARROWER));

        assertEquals(ShapeFixtures.torus(NARROWER), derived.getShape());
        assertEquals(carried.getOptions(), derived.getOptions());
    }

    @Test
    void aShapeTooNarrowToGenerateIsRefusedWithTheVerifiersMessage() {
        FlatShape narrow = ShapeFixtures.torus(TOO_NARROW);

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new CarriedShape(narrow, ShapeOptions.DEFAULT));

        assertEquals(WorldFolds.verifyGeneratable(narrow).error().orElseThrow().message(), refused.getMessage());
    }

    @Test
    void aDerivedShapeIsHeldToTheSameFloor() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE), ShapeOptions.DEFAULT);

        assertThrows(IllegalArgumentException.class, () -> carried.withShape(ShapeFixtures.torus(TOO_NARROW)));
    }
}
