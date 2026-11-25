package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class ShapedDimensionsTest {
    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createWorldLookup();

    private static final FlatShape TORUS = ShapeFixtures.torus(WorldLoopBounds.ofWidth(32));

    @Test
    void aDerivedStemScalesTheSkewWithTheWidths() {
        FlatShape overworld = FlatShape.latticeTorus(WorldLoopBounds.ofWidths(256, 128), 16);

        assertEquals(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(32, 16), 2),
                ShapedDimensions.derivedShape(overworld, 1.0, 8.0));
    }

    @Test
    void aSkewThatScalesToNoWholeChunkDerivesNothing() {
        FlatShape overworld = FlatShape.latticeTorus(WorldLoopBounds.ofWidths(256, 128), 12);

        assertNull(ShapedDimensions.derivedShape(overworld, 1.0, 8.0));
    }

    @Test
    void anOverworldThatRefusesTheShapeLeavesTheNetherUntouched() {
        WorldDimensions foreign = overworldAndNetherOf(
                new ForeignChunkGenerator(plainsBiomeSource(), overworldNoiseSettings()),
                new NoiseBasedChunkGenerator(plainsBiomeSource(), overworldNoiseSettings()));

        WorldDimensions shaped = ShapedDimensions.withShapes(foreign,
                new CarriedShape(null, TORUS, ShapeOptions.DEFAULT), new CarriedShape(null, TORUS, ShapeOptions.DEFAULT),
                        new CarriedShape(null, TORUS, ShapeOptions.DEFAULT));

        assertSame(foreign, shaped);
        assertNull(ShapedDimensions.getShape(shaped, LevelStem.NETHER));
    }

    @Test
    void anUnshapedChosenPresetLeavesTheResult() {
        WorldDimensions chosen = overworldOf(new NoiseBasedChunkGenerator(
                plainsBiomeSource(), overworldNoiseSettings()));
        WorldDimensions created = overworldOf(new ForeignChunkGenerator(
                plainsBiomeSource(), overworldNoiseSettings()));

        assertSame(created, ShapedDimensions.keepChosenShape(created, chosen));
    }

    private static WorldDimensions overworldOf(ChunkGenerator generator) {
        return new WorldDimensions(Map.of(LevelStem.OVERWORLD, new LevelStem(
                WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                generator)));
    }

    private static WorldDimensions overworldAndNetherOf(ChunkGenerator overworld, ChunkGenerator nether) {
        return new WorldDimensions(Map.of(
                LevelStem.OVERWORLD, new LevelStem(
                        WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                        overworld),
                LevelStem.NETHER, new LevelStem(
                        WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.NETHER),
                        nether)));
    }

    private static BiomeSource plainsBiomeSource() {
        return new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
    }

    private static Holder<NoiseGeneratorSettings> overworldNoiseSettings() {
        return WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
    }

    private static class ForeignChunkGenerator extends NoiseBasedChunkGenerator {
        ForeignChunkGenerator(BiomeSource biomes, Holder<NoiseGeneratorSettings> settings) {
            super(biomes, settings);
        }
    }
}
