package com.exoticworlds.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.network.VarInt;

class WorldLoopBoundsTest {
    private static final int LEGACY_DISABLED_AXIS_RADIUS_CHUNKS = 1883191;

    @Test
    void codecAcceptsARealSpan() {
        assertEquals(new AxisBounds.Looped(-32, 32), readAxis("{\"min_chunk\":-32,\"max_chunk\":32}"));
    }

    @Test
    void codecAcceptsAnAbsentPairAsUnbounded() {
        assertEquals(AxisBounds.Unbounded.INSTANCE, readAxis("{}"));
    }

    @Test
    void codecAcceptsTheLegacySentinelAsUnbounded() {
        int radiusChunks = LEGACY_DISABLED_AXIS_RADIUS_CHUNKS;
        assertEquals(AxisBounds.Unbounded.INSTANCE,
                readAxis("{\"min_chunk\":" + -radiusChunks + ",\"max_chunk\":" + radiusChunks + "}"));
        assertEquals(AxisBounds.Unbounded.INSTANCE,
                readAxis("{\"min_chunk\":" + -radiusChunks + ",\"max_chunk\":16}"));
        assertEquals(AxisBounds.Unbounded.INSTANCE,
                readAxis("{\"min_chunk\":-16,\"max_chunk\":" + radiusChunks + "}"));
    }

    @Test
    void codecRejectsAZeroWidthSpan() {
        String error = readAxisError("{\"min_chunk\":5,\"max_chunk\":5}");
        assertTrue(error.contains("[5, 5)"), error);
    }

    @Test
    void codecRejectsANegativeWidthSpan() {
        String error = readAxisError("{\"min_chunk\":10,\"max_chunk\":-10}");
        assertTrue(error.contains("[10, -10)"), error);
    }

    @Test
    void codecRejectsAOneSidedPair() {
        String error = readAxisError("{\"min_chunk\":0}");
        assertTrue(error.contains("both chunk bounds or neither"), error);
    }

    @Test
    void codecWritesALoopedSpanAsItsTwoBounds() {
        assertEquals(JsonParser.parseString("{\"min_chunk\":-32,\"max_chunk\":32}"),
                writeAxis(new AxisBounds.Looped(-32, 32)));
    }

    @Test
    void codecWritesAnUnboundedAxisAsAnAbsentPair() {
        assertEquals(JsonParser.parseString("{}"), writeAxis(AxisBounds.Unbounded.INSTANCE));
    }

    @Test
    void aLoopedAxisAnswersBlockQueriesFromItsChunkBounds() {
        AxisBounds.Looped axis = new AxisBounds.Looped(-32, 32);

        assertEquals(-32 * 16, axis.getMinBlock());
        assertEquals(32 * 16, axis.getMaxBlock());
    }

    @Test
    void maxViewDistanceIsHalfTheNarrowerLoopedWidthMinusThreeAndNeverBelowOne() {
        assertEquals(64 / 2 - 3, WorldLoopBounds.ofWidth(64).getMaxViewDistanceChunks());
        assertEquals(16 / 2 - 3, ShapeFixtures.loopedBounds(-48, 16, 0, 16).getMaxViewDistanceChunks());
        assertEquals(1, ShapeFixtures.loopedBounds(-2, 3, -2, 3).getMaxViewDistanceChunks());
        assertEquals(64 / 2 - 3, new WorldLoopBounds(
                new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE).getMaxViewDistanceChunks());
        assertEquals(Integer.MAX_VALUE, WorldLoopBounds.UNBOUNDED.getMaxViewDistanceChunks());
    }

    @Test
    void axisRoundTripsThroughItsOwnWriting() {
        List<AxisBounds> axes = List.of(
                new AxisBounds.Looped(-32, 32),
                new AxisBounds.Looped(0, 64),
                new AxisBounds.Looped(-48, 16),
                AxisBounds.Unbounded.INSTANCE);
        for (AxisBounds axis : axes) {
            assertEquals(axis, AxisBounds.CODEC.parse(JsonOps.INSTANCE, writeAxis(axis)).getOrThrow());
        }
    }

    @Test
    void worldBoundsWriteUnderTheirAxisKeysAndRoundTrip() {
        WorldLoopBounds mixed = new WorldLoopBounds(new AxisBounds.Looped(-16, 48), AxisBounds.Unbounded.INSTANCE);
        JsonElement written = WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, mixed).getOrThrow();

        assertEquals(JsonParser.parseString("{\"x\":{\"min_chunk\":-16,\"max_chunk\":48},\"z\":{}}"), written);
        assertEquals(mixed, WorldLoopBounds.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void worldBoundsRoundTripEveryAxisShape() {
        List<WorldLoopBounds> shapes = List.of(
                WorldLoopBounds.ofWidth(32),
                WorldLoopBounds.ofWidth(5),
                WorldLoopBounds.UNBOUNDED,
                new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16)),
                ShapeFixtures.loopedBounds(-16, 16, -32, 32));
        for (WorldLoopBounds bounds : shapes) {
            JsonElement written = WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, bounds).getOrThrow();
            assertEquals(bounds, WorldLoopBounds.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow(),
                    written.toString());
        }
    }

    private static JsonElement writeAxis(AxisBounds axis) {
        return AxisBounds.CODEC.encodeStart(JsonOps.INSTANCE, axis).getOrThrow();
    }

    private static AxisBounds readAxis(String json) {
        return AxisBounds.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static String readAxisError(String json) {
        DataResult<AxisBounds> result = AxisBounds.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.isError());
        return result.error().orElseThrow().message();
    }
    @Test
    void aCreationFlowWorldIsSquare() {
        assertTrue(WorldLoopBounds.ofWidth(32).isSquare());
        assertTrue(WorldLoopBounds.ofWidth(5).isSquare());
    }

    @Test
    void equalWidthsPlacedDifferentlyPerAxisAreStillSquare() {
        assertTrue(ShapeFixtures.loopedBounds(-48, 16, 0, 64).isSquare());
    }

    @Test
    void aSingleAxisLoopIsNotSquare() {
        assertFalse(new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE).isSquare());
        assertFalse(new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16)).isSquare());
        assertFalse(WorldLoopBounds.UNBOUNDED.isSquare());
    }

    @Test
    void aRectangularWorldIsNotSquare() {
        assertFalse(ShapeFixtures.loopedBounds(-16, 16, -32, 32).isSquare());
    }

    @Test
    void aLoopedAxisRefusesAnEmptyOrReversedSpan() {
        assertThrows(IllegalArgumentException.class, () -> new AxisBounds.Looped(5, 5));
        assertThrows(IllegalArgumentException.class, () -> new AxisBounds.Looped(10, -10));
    }

    @Test
    void theWireRefusesAnEmptySpan() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBoolean(true);
        VarInt.write(buffer, 5);
        VarInt.write(buffer, 5);

        assertThrows(IllegalArgumentException.class, () -> AxisBounds.STREAM_CODEC.decode(buffer));
    }

    @Test
    void ofWidthsCentersEachAxisOnItsOwnWidthAndTheNarrowerAxisBoundsTheView() {
        WorldLoopBounds rectangle = WorldLoopBounds.ofWidths(64, 128);

        assertEquals(ShapeFixtures.loopedBounds(-32, 32, -64, 64), rectangle);
        assertFalse(rectangle.isSquare());
        assertEquals(WorldLoopBounds.ofWidth(32), WorldLoopBounds.ofWidths(32, 32));
        assertEquals(WorldLoopBounds.ofWidth(64).getMaxViewDistanceChunks(), rectangle.getMaxViewDistanceChunks());
        assertEquals(WorldLoopBounds.ofWidths(32, 64), rectangle.scaledDown(2));
    }

    @Test
    void ofWidthOnOneAxisLoopsThatAxisCenteredAndLeavesTheOtherUnbounded() {
        assertEquals(new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE),
                WorldLoopBounds.ofWidth(Direction.Axis.X, 32));
        assertEquals(new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-2, 3)),
                WorldLoopBounds.ofWidth(Direction.Axis.Z, 5));
        assertThrows(IllegalArgumentException.class, () -> WorldLoopBounds.ofWidth(Direction.Axis.Y, 32));
    }

    @Test
    void perAxisReadsAnswerForTheAxisNamed() {
        WorldLoopBounds xOnly = WorldLoopBounds.ofWidth(Direction.Axis.X, 32);

        assertEquals(new AxisBounds.Looped(-16, 16), xOnly.getAxis(Direction.Axis.X));
        assertEquals(AxisBounds.Unbounded.INSTANCE, xOnly.getAxis(Direction.Axis.Z));
        assertTrue(xOnly.isLooped(Direction.Axis.X));
        assertFalse(xOnly.isLooped(Direction.Axis.Z));
        assertEquals(32, xOnly.getWidthChunks(Direction.Axis.X));
        assertThrows(IllegalStateException.class, () -> xOnly.getWidthChunks(Direction.Axis.Z));
        assertThrows(IllegalArgumentException.class, () -> xOnly.getAxis(Direction.Axis.Y));

        WorldLoopBounds rectangle = ShapeFixtures.loopedBounds(-16, 16, -32, 32);
        assertEquals(32, rectangle.getWidthChunks(Direction.Axis.X));
        assertEquals(64, rectangle.getWidthChunks(Direction.Axis.Z));
    }

    @Test
    void scaledDownDividesEveryLoopedWidthRecenteredAndLeavesAnUnboundedAxisAlone() {
        assertEquals(WorldLoopBounds.ofWidth(16), WorldLoopBounds.ofWidth(32).scaledDown(2));
        assertEquals(WorldLoopBounds.ofWidth(32), ShapeFixtures.loopedBounds(-48, 16, 0, 64).scaledDown(2));
        assertEquals(WorldLoopBounds.ofWidth(Direction.Axis.Z, 16),
                WorldLoopBounds.ofWidth(Direction.Axis.Z, 128).scaledDown(8));
        assertEquals(ShapeFixtures.loopedBounds(-8, 8, -32, 32),
                ShapeFixtures.loopedBounds(-16, 16, -64, 64).scaledDown(2));
        assertEquals(WorldLoopBounds.UNBOUNDED, WorldLoopBounds.UNBOUNDED.scaledDown(8));
        assertEquals(WorldLoopBounds.ofWidth(32), WorldLoopBounds.ofWidth(32).scaledDown(1));
    }
}
