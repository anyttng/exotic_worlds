package com.exoticworlds.api.v1.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.shape.StartupRegistrations;
import com.mojang.serialization.Codec;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class ShapeDimensionsTest {
    private static final int AFTER_THE_BUILT_IN_OPTIONS = 500;

    private static final int STORED_DEFAULT = 0;

    private static final int CHOSEN = 9;

    private static final int OVERWORLD_WIDTH_CHUNKS = 64;

    private static final int NETHER_SCALE = 4;

    private static final int END_WIDTH_CHUNKS = 320;

    private static final WorldOption<Integer> MOD_OPTION =
            new WorldOption<>("example_mod_v1_shape_dimensions_test", AFTER_THE_BUILT_IN_OPTIONS, Codec.INT,
                    STORED_DEFAULT);

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void registerTheModsOption() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        WorldOptions.register(MOD_OPTION);
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aRegisteredOptionIsListedWithTheBuiltInOnes() {
        assertTrue(WorldOptions.all().contains(MOD_OPTION));
    }

    @Test
    void anOptionWrittenWithTheSpansReadsBackFromEveryStem() {
        LoopSpans overworld = LoopSpans.ofWidth(OVERWORLD_WIDTH_CHUNKS);
        GenerationOptions chosen = GenerationOptions.DEFAULT.with(MOD_OPTION, CHOSEN);

        WorldDimensions made = ShapeDimensions.withSpans(vanillaDimensions(), overworld,
                overworld.scaledDown(NETHER_SCALE), LoopSpans.ofWidth(END_WIDTH_CHUNKS), chosen);

        for (ResourceKey<LevelStem> stem : List.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END)) {
            assertEquals(CHOSEN, ShapeDimensions.optionsOf(made, stem).get(MOD_OPTION), stem.toString());
        }

        assertEquals(overworld, ShapeDimensions.spansOf(made, LevelStem.OVERWORLD));
        assertEquals(overworld.scaledDown(NETHER_SCALE), ShapeDimensions.spansOf(made, LevelStem.NETHER));
    }

    @Test
    void aWorldWithoutAShapeReadsNoSpansAndTheDefaultOptions() {
        WorldDimensions vanilla = vanillaDimensions();

        assertNull(ShapeDimensions.spansOf(vanilla, LevelStem.OVERWORLD));
        assertEquals(GenerationOptions.DEFAULT, ShapeDimensions.optionsOf(vanilla, LevelStem.OVERWORLD));
    }

    private static WorldDimensions vanillaDimensions() {
        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>();
        stems.put(LevelStem.OVERWORLD, stem(BuiltinDimensionTypes.OVERWORLD, NoiseGeneratorSettings.OVERWORLD));
        stems.put(LevelStem.NETHER, stem(BuiltinDimensionTypes.NETHER, NoiseGeneratorSettings.NETHER));
        stems.put(LevelStem.END, stem(BuiltinDimensionTypes.END, NoiseGeneratorSettings.END));
        return new WorldDimensions(stems);
    }

    private static LevelStem stem(ResourceKey<DimensionType> type, ResourceKey<NoiseGeneratorSettings> noise) {
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(worldgen.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                worldgen.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(noise));
        return new LevelStem(worldgen.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(type), generator);
    }
}
