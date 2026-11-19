package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

class CarriedShapeTest {
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds NARROWER = WorldLoopBounds.ofWidth(16);
    private static final WorldLoopBounds TOO_NARROW = WorldLoopBounds.ofWidth(WorldLoopSizes.MIN_CHUNK_WIDTH - 2);

    private static final String NO_PREFIX = "";
    private static final String STAMP_PREFIX = ExoticWorlds.MODID + ":";

    private static final List<String> PREFIXES = List.of(NO_PREFIX, STAMP_PREFIX);

    @Test
    void everyPrefixCarriesTheShapeAndTheChosenOptionsBackUnchanged() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF));

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
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE), GenerationOptions.DEFAULT);

        for (String prefix : PREFIXES) {
            MapCodec<CarriedShape> codec = CarriedShape.mapCodec(prefix);
            JsonElement written = codec.codec().encodeStart(JsonOps.INSTANCE, carried).getOrThrow();

            assertFalse(written.getAsJsonObject().has(prefix + CompactBiomes.KEY), written.toString());
            assertEquals(carried, codec.codec().parse(JsonOps.INSTANCE, written).getOrThrow(), written.toString());
        }
    }

    @Test
    void theFoldIsBuiltOnceAndHandedBackAsTheSameReference() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE), GenerationOptions.DEFAULT);

        assertSame(carried.fold(), carried.fold());
        assertTrue(carried.fold().isWrapped());
    }

    @Test
    void aDerivedShapeKeepsTheOptionsItWasCarriedWith() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF));

        CarriedShape derived = carried.withShape(ShapeFixtures.torus(NARROWER));

        assertEquals(ShapeFixtures.torus(NARROWER), derived.shape());
        assertEquals(carried.generationOptions(), derived.generationOptions());
    }

    @Test
    void aShapeTooNarrowToGenerateIsRefusedWithTheVerifiersMessage() {
        FlatShape narrow = ShapeFixtures.torus(TOO_NARROW);

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new CarriedShape(narrow, GenerationOptions.DEFAULT));

        assertEquals(WorldFolds.verifyGeneratable(narrow).error().orElseThrow().message(), refused.getMessage());
    }

    @Test
    void aDerivedShapeIsHeldToTheSameFloor() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE), GenerationOptions.DEFAULT);

        assertThrows(IllegalArgumentException.class, () -> carried.withShape(ShapeFixtures.torus(TOO_NARROW)));
    }
}
