package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

class SpawnPointTest {
    private static final Codec<OptionValues> OPTIONS = OptionValues.mapCodec("").codec();

    @Test
    void aWorldFileWithoutASpawnPointReadsAsDefault() {
        OptionValues read = OPTIONS.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();

        assertEquals(SpawnPoint.DEFAULT, read.get(SpawnPoint.OPTION));
        assertEquals(JsonParser.parseString("{}"), OPTIONS.encodeStart(JsonOps.INSTANCE, read).getOrThrow());
    }

    @Test
    void aPointAtChunkZeroIsStoredAndReadBackAsAPoint() {
        OptionValues chosen = OptionValues.DEFAULT.with(SpawnPoint.OPTION, new SpawnPoint(0, 0));
        JsonElement written = OPTIONS.encodeStart(JsonOps.INSTANCE, chosen).getOrThrow();

        assertEquals(JsonParser.parseString("{\"spawn\":{\"x\":0,\"z\":0}}"), written);
        assertTrue(OPTIONS.parse(JsonOps.INSTANCE, written).getOrThrow().get(SpawnPoint.OPTION).isCustom());
    }

    @Test
    void theDefaultHasNoCoordinatesOfItsOwn() {
        assertEquals(SpawnPoint.DEFAULT, new SpawnPoint(SpawnPoint.Mode.DEFAULT, 5, -7));
    }

    @Test
    void aSpawnPointIsWrittenInChunksAndReadBack() {
        OptionValues chosen = OptionValues.DEFAULT.with(SpawnPoint.OPTION, new SpawnPoint(2, -3));
        JsonElement written = OPTIONS.encodeStart(JsonOps.INSTANCE, chosen).getOrThrow();

        assertEquals(JsonParser.parseString("{\"spawn\":{\"x\":2,\"z\":-3}}"), written);
        assertEquals(chosen, OPTIONS.parse(JsonOps.INSTANCE, written).getOrThrow());
    }
}
