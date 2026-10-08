package com.exoticworlds.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.shape.cylinder.CylinderSettings;
import com.exoticworlds.shape.torus.TorusSettings;

import net.minecraft.core.Direction;

class WorldLoopPresetsJsonTest {
    private static final String PRESET_RESOURCE_DIR = "/data/exotic_worlds/worldgen/world_preset/";
    private static final String LOOPED_GENERATOR_ID = "exotic_worlds:toroidal";
    private static final String CYLINDER_PRESET_PREFIX = "cylinder_";

    @Test
    void everyPresetShipsATorusWorldPresetMatchingItsConfiguration() throws IOException {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(preset.id(), preset, WorldLoopBounds::ofWidth, TorusSettings.OFFERED_OPTIONS,
                    TorusSettings.DEFAULT.generationOptions());
        }
    }

    @Test
    void everyPresetShipsACylinderWorldPresetOnTheDefaultAxis() throws IOException {
        Direction.Axis axis = CylinderSettings.DEFAULT.axis();
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(CYLINDER_PRESET_PREFIX + preset.id(), preset, width -> WorldLoopBounds.ofWidth(axis, width),
                    CylinderSettings.OFFERED_OPTIONS, CylinderSettings.DEFAULT.generationOptions());
        }
    }

    private static void assertPreset(String presetId, WorldLoopPresets preset, IntFunction<WorldLoopBounds> boundsOfWidth,
            List<WorldOption<?>> offered, GenerationOptions defaults) throws IOException {
        JsonObject dimensions = readPresetJson(presetId).getAsJsonObject("dimensions");
        assertNotNull(dimensions, presetId + ": no dimensions object");
        assertEquals(3, dimensions.size(), presetId + ": expected exactly the three vanilla dimensions");

        assertDimension(presetId, dimensions, "minecraft:overworld", "minecraft:overworld",
                boundsOfWidth.apply(preset.chunkWidth()), offered, defaults);
        assertDimension(presetId, dimensions, "minecraft:the_nether", "minecraft:nether",
                boundsOfWidth.apply(NetherScales.netherChunkWidth(preset.chunkWidth(), preset.netherScale())),
                offered, defaults);
        assertDimension(presetId, dimensions, "minecraft:the_end", "minecraft:end",
                boundsOfWidth.apply(preset.endChunkWidth()), offered, defaults);
    }

    private static void assertDimension(String presetId, JsonObject dimensions, String dimensionId,
            String noiseSettingsId, WorldLoopBounds expected, List<WorldOption<?>> offered,
            GenerationOptions defaults) {
        String context = presetId + " " + dimensionId;
        JsonObject dimension = dimensions.getAsJsonObject(dimensionId);
        assertNotNull(dimension, context + ": dimension missing");
        assertEquals(dimensionId, dimension.get("type").getAsString(), context + ": dimension type");

        JsonObject generator = dimension.getAsJsonObject("generator");
        assertEquals(LOOPED_GENERATOR_ID, generator.get("type").getAsString(), context + ": generator type");
        assertEquals(noiseSettingsId, generator.get("settings").getAsString(), context + ": noise settings");

        WorldLoopBounds wrapping = WorldLoopBounds.CODEC.parse(JsonOps.INSTANCE, generator.get("wrapping"))
                .getOrThrow(message -> new AssertionError(context + ": wrapping does not parse: " + message));
        assertEquals(expected, wrapping, context + ": wrapping");

        for (WorldOption<?> option : WorldOptions.all()) {
            if (offered.contains(option)) {
                assertStatedOption(context, generator, option, defaults);
            } else {
                assertFalse(generator.has(option.key()), context + ": the shape offers no " + option.key());
            }
        }
    }

    private static <V> void assertStatedOption(String context, JsonObject generator, WorldOption<V> option,
            GenerationOptions defaults) {
        JsonElement stated = generator.get(option.key());
        assertNotNull(stated, context + ": " + option.key() + " is not stated");

        V value = option.codec().parse(JsonOps.INSTANCE, stated)
                .getOrThrow(message -> new AssertionError(context + ": " + option.key() + " does not parse: " + message));
        assertEquals(defaults.get(option), value, context + ": " + option.key());
    }

    private static JsonObject readPresetJson(String presetId) throws IOException {
        String resource = PRESET_RESOURCE_DIR + presetId + ".json";
        try (InputStream stream = WorldLoopPresetsJsonTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "missing jar resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
