package com.exoticworlds.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;
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
    void aSkewedTorusCarriesTheSkewIntoTheNetherAtTheScaleAndLeavesTheEndPlain() {
        TorusSettings chosen = new TorusSettings(LoopSpans.ofWidths(64, 128).withSkew(8), 4, LoopSpans.ofWidth(320),
                TorusSettings.DEFAULT.generationOptions());
        WorldDimensions created = TorusDimensions.apply(vanillaDimensions(), chosen);

        assertEquals(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(64, 128), 8),
                ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertEquals(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(16, 32), 2),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(FlatShape.torus(WorldLoopBounds.ofWidth(320)),
                ShapedDimensions.shapeOf(created, LevelStem.END));
        assertEquals(chosen, TorusDimensions.read(created));
    }

    @Test
    void aNetherScaleTheSkewDoesNotDivideFallsToOneThatDoes() {
        TorusSettings chosen = new TorusSettings(LoopSpans.ofWidth(256).withSkew(4), 8, LoopSpans.ofWidth(256),
                TorusSettings.DEFAULT.generationOptions());
        WorldDimensions created = TorusDimensions.apply(vanillaDimensions(), chosen);

        assertEquals(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(64), 1),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(4, TorusDimensions.read(created).netherScale());
    }

    @Test
    void aSkewPastHalfTheWidthReadsBackAsTheSameLattice() {
        TorusSettings chosen = new TorusSettings(LoopSpans.ofWidths(64, 128).withSkew(60), 1, LoopSpans.ofWidth(320),
                TorusSettings.DEFAULT.generationOptions());
        TorusSettings read = TorusDimensions.read(TorusDimensions.apply(vanillaDimensions(), chosen));

        assertEquals(-4, read.skewChunks());
        assertEquals(60, Math.floorMod(read.skewChunks(), read.chunkWidth(Direction.Axis.X)));
    }

    @Test
    void aSkewedEndReadsBackAsTheDefaultEnd() {
        WorldDimensions created = TorusDimensions.apply(vanillaDimensions(), RECTANGLE_64_BY_128);
        WorldDimensions skewedEnd = ShapedDimensions.withShape(created, LevelStem.END, new CarriedShape(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidth(320), 5), RECTANGLE_64_BY_128.generationOptions()));

        assertEquals(TorusSettings.DEFAULT.end(), TorusDimensions.read(skewedEnd).end());
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
