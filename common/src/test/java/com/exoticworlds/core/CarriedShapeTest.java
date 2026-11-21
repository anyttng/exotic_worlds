package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

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

    private static final CuboidShape CUBOID = new CuboidShape(16, 24, 40);
    private static final int UNDER_THE_FLOOR = WorldLoopSizes.MIN_CHUNK_WIDTH - 1;
    private static final Map<String, CuboidShape> NARROW_CUBOIDS = Map.of(
            "a_chunks", new CuboidShape(UNDER_THE_FLOOR, 24, 40),
            "b_chunks", new CuboidShape(16, UNDER_THE_FLOOR, 40),
            "c_chunks", new CuboidShape(16, 24, UNDER_THE_FLOOR));

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
    void aCuboidComesBackUnderEveryPrefixWithAFoldOfItsThreeSizes() {
        CarriedShape carried = new CarriedShape(CUBOID, GenerationOptions.DEFAULT);

        for (String prefix : PREFIXES) {
            MapCodec<CarriedShape> codec = CarriedShape.mapCodec(prefix);
            JsonElement written = codec.codec().encodeStart(JsonOps.INSTANCE, carried).getOrThrow();
            CarriedShape read = codec.codec().parse(JsonOps.INSTANCE, written).getOrThrow();

            assertEquals(carried, read, written.toString());
            assertEquals(CUBOID, read.shape(), written.toString());
            assertFrontAtTheOrigin(read.fold().cuboidAtlas());
        }
    }

    @Test
    void aCuboidEdgeUnderTheFloorFailsToDecodeNamingTheEdge() {
        for (Map.Entry<String, CuboidShape> narrow : NARROW_CUBOIDS.entrySet()) {
            JsonElement written = ShapeModel.CODEC.encodeStart(JsonOps.INSTANCE, narrow.getValue()).getOrThrow();

            String refusal = CarriedShape.SHAPE_CODEC.parse(JsonOps.INSTANCE, written).error().orElseThrow().message();

            assertTrue(refusal.contains(narrow.getKey()), refusal);
        }
    }

    @Test
    void aCuboidEdgeUnderTheFloorIsRefusedByTheConstructorNamingTheEdge() {
        for (Map.Entry<String, CuboidShape> narrow : NARROW_CUBOIDS.entrySet()) {
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> new CarriedShape(narrow.getValue(), GenerationOptions.DEFAULT));

            assertTrue(refused.getMessage().contains(narrow.getKey()), refused.getMessage());
        }
    }

    private static void assertFrontAtTheOrigin(CuboidAtlas atlas) {
        assertNotNull(atlas);
        CuboidFace front = atlas.faceAt(0, 0);
        assertNotNull(front);
        assertEquals(CuboidFace.Side.FRONT, front.side());
        assertEquals(CUBOID.aChunks() * CoordinateConstants.CHUNK_WIDTH, front.widthBlocks());
        assertEquals(CUBOID.bChunks() * CoordinateConstants.CHUNK_WIDTH, front.depthBlocks());
        assertEquals(-(CUBOID.aChunks() / 2) * CoordinateConstants.CHUNK_WIDTH, front.minBlockX());
        assertEquals(-(CUBOID.bChunks() / 2) * CoordinateConstants.CHUNK_WIDTH, front.minBlockZ());

        CuboidFace top = atlas.faceAt(0, front.minBlockZ() - 1);
        assertNotNull(top);
        assertEquals(CuboidFace.Side.TOP, top.side());
        assertEquals(CUBOID.cChunks() * CoordinateConstants.CHUNK_WIDTH, top.depthBlocks());
    }

    @Test
    void aDerivedShapeIsHeldToTheSameFloor() {
        CarriedShape carried = new CarriedShape(ShapeFixtures.torus(SQUARE), GenerationOptions.DEFAULT);

        assertThrows(IllegalArgumentException.class, () -> carried.withShape(ShapeFixtures.torus(TOO_NARROW)));
    }
}
