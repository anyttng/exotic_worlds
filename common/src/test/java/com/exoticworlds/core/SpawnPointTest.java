package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.shape.WorldOptionSetup;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

class SpawnPointTest {
    private static final Codec<GenerationOptions> OPTIONS = GenerationOptions.mapCodec("").codec();

    @BeforeAll
    static void registerTheOptions() {
        WorldOptionSetup.registerAll(false);
    }

    @Test
    void aWorldFileWithoutASpawnPointReadsAsZero() {
        GenerationOptions read = OPTIONS.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();

        assertEquals(SpawnPoint.ZERO, read.get(SpawnPoint.OPTION));
    }

    @Test
    void aSpawnPointIsWrittenInChunksAndReadBack() {
        GenerationOptions chosen = GenerationOptions.DEFAULT.with(SpawnPoint.OPTION, new SpawnPoint(2, -3));
        JsonElement written = OPTIONS.encodeStart(JsonOps.INSTANCE, chosen).getOrThrow();

        assertEquals(JsonParser.parseString("{\"spawn\":{\"x\":2,\"z\":-3}}"), written);
        assertEquals(chosen, OPTIONS.parse(JsonOps.INSTANCE, written).getOrThrow());
    }
}
