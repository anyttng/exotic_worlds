package com.exoticworlds.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class TorusDimensionsTest {
    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createWorldLookup();

    private static final TorusSettings RECTANGLE_64_BY_128 = new TorusSettings(
            LoopSpans.ofWidths(64, 128), 4, LoopSpans.ofWidth(320), TorusSettings.DEFAULT.generationOptions());

    @Test
    void aRectangleRoundTripsThroughItsThreeGenerators() {
        WorldDimensions created = TorusDimensions.apply(vanillaDimensions(), RECTANGLE_64_BY_128);

        assertEquals(FlatShape.torus(WorldLoopBounds.ofWidths(64, 128)),
                ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertEquals(FlatShape.torus(WorldLoopBounds.ofWidths(16, 32)),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(FlatShape.torus(WorldLoopBounds.ofWidth(320)),
                ShapedDimensions.shapeOf(created, LevelStem.END));
        assertEquals(RECTANGLE_64_BY_128, TorusDimensions.read(created));
    }

    @Test
    void aNetherScaleOneAxisCannotTakeFallsToOneBothAxesAdmit() {
        TorusSettings chosen = new TorusSettings(LoopSpans.ofWidths(256, 48), 8, LoopSpans.ofWidth(256),
                TorusSettings.DEFAULT.generationOptions());
        WorldDimensions created = TorusDimensions.apply(vanillaDimensions(), chosen);

        assertEquals(FlatShape.torus(WorldLoopBounds.ofWidths(128, 24)),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(2, TorusDimensions.read(created).netherScale());
    }

    @Test
    void aSquareStillRoundTrips() {
        WorldDimensions created = TorusDimensions.apply(vanillaDimensions(), TorusSettings.DEFAULT);

        assertEquals(TorusSettings.DEFAULT.overworld(), TorusDimensions.read(created).overworld());
    }

    @Test
    void anOrdinaryWorldIsNotClaimed() {
        assertNull(TorusDimensions.read(vanillaDimensions()));
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
                new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(noise));
        return new LevelStem(WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(type), generator);
    }
}
