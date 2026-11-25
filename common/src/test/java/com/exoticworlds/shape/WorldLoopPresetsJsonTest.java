package com.exoticworlds.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;

import net.minecraft.core.Direction;

class WorldLoopPresetsJsonTest {
    private static final String PRESET_RESOURCE_DIR = "/data/exotic_worlds/worldgen/world_preset/";
    private static final String LOOPED_GENERATOR_ID = "exotic_worlds:toroidal";
    private static final String CYLINDER_PRESET_PREFIX = "cylinder_";
    private static final String TORUS_PRESET_PREFIX = "torus_";
    private static final String PRESET_FILE_EXTENSION = ".json";
    private static final List<ShapeOption<?>> TORUS_OPTIONS =
            List.of(CompactBiomes.FEATURE.getOption(), GuaranteedLand.FEATURE.getOption(), GuaranteedNetherComplexes.FEATURE.getOption());
    private static final List<ShapeOption<?>> CYLINDER_OPTIONS = List.of(CompactBiomes.FEATURE.getOption());

    @Test
    void everyPresetShipsATorusWorldPresetMatchingItsConfiguration() throws IOException {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(TORUS_PRESET_PREFIX + preset.getId(), preset, WorldLoopBounds::ofWidth,
                    TORUS_OPTIONS, DimensionLayout.DEFAULT_OPTIONS);
        }
    }

    @Test
    void theEndColumnFollowsTheWidthRule() {
        assertEquals(List.of(208, 240, 304, 432, 688),
                Arrays.stream(WorldLoopPresets.values()).map(WorldLoopPresets::getEndWidthChunks).toList());
    }

    @Test
    void noPresetAnswersToABareSize() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            String resource = PRESET_RESOURCE_DIR + preset.getId() + PRESET_FILE_EXTENSION;
            assertNull(WorldLoopPresetsJsonTest.class.getResource(resource), "a bare size preset ships: " + resource);
        }
    }

    @Test
    void everyPresetShipsACylinderWorldPresetOnTheDefaultAxis() throws IOException {
        Direction.Axis axis = Direction.Axis.X;
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(CYLINDER_PRESET_PREFIX + preset.getId(), preset, width -> WorldLoopBounds.ofWidth(axis, width),
                    CYLINDER_OPTIONS, DimensionLayout.DEFAULT_OPTIONS);
        }
    }

    private static void assertPreset(String presetId, WorldLoopPresets preset, IntFunction<WorldLoopBounds> boundsOfWidth,
            List<ShapeOption<?>> offered, ShapeOptions defaults) throws IOException {
        JsonObject dimensions = readPresetJson(presetId).getAsJsonObject("dimensions");
        assertNotNull(dimensions, presetId + ": no dimensions object");
        assertEquals(3, dimensions.size(), presetId + ": expected exactly the three vanilla dimensions");

        assertDimension(presetId, dimensions, "minecraft:overworld", "minecraft:overworld",
                boundsOfWidth.apply(preset.getWidthChunks()), offered, defaults);
        assertDimension(presetId, dimensions, "minecraft:the_nether", "minecraft:nether",
                boundsOfWidth.apply(preset.getWidthChunks() / preset.getNetherScale()),
                offered, defaults);
        assertDimension(presetId, dimensions, "minecraft:the_end", "minecraft:end",
                boundsOfWidth.apply(preset.getEndWidthChunks()), offered, defaults);
    }

    private static void assertDimension(String presetId, JsonObject dimensions, String dimensionId,
            String noiseSettingsId, WorldLoopBounds expected, List<ShapeOption<?>> offered,
            ShapeOptions defaults) {
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

        for (ShapeOption<?> option : ShapeOptionRegistry.getAll()) {
            if (offered.contains(option)) {
                assertStatedOption(context, generator, option, defaults);
            } else {
                assertFalse(generator.has(option.key()), context + ": the shape offers no " + option.key());
            }
        }
    }

    private static <V> void assertStatedOption(String context, JsonObject generator, ShapeOption<V> option,
            ShapeOptions defaults) {
        JsonElement stated = generator.get(option.key());
        assertNotNull(stated, context + ": " + option.key() + " is not stated");

        V value = option.codec().parse(JsonOps.INSTANCE, stated)
                .getOrThrow(message -> new AssertionError(context + ": " + option.key() + " does not parse: " + message));
        assertEquals(defaults.get(option), value, context + ": " + option.key());
    }

    private static JsonObject readPresetJson(String presetId) throws IOException {
        String resource = PRESET_RESOURCE_DIR + presetId + PRESET_FILE_EXTENSION;
        try (InputStream stream = WorldLoopPresetsJsonTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "missing jar resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
