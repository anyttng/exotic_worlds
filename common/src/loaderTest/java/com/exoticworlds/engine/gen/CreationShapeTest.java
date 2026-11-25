package com.exoticworlds.engine.gen;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.layout.LayoutDimensions;
import com.exoticworlds.shape.layout.LayoutFixtures;
import com.exoticworlds.shape.layout.WorldLayout;
import static com.exoticworlds.engine.gen.BakeStampFixture.foreignGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.noiseGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.noiseSubclassGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.selected;
import static com.exoticworlds.engine.gen.BakeStampFixture.squareTorus;
import static com.exoticworlds.engine.gen.BakeStampFixture.stem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;


import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

@Timeout(60)
class CreationShapeTest {
    private static final double OVERWORLD_SCALE = 1.0;
    private static final double NETHER_COORDINATE_SCALE = 8.0;

    private static final int CHOSEN_WIDTH_CHUNKS = 64;

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createWorldLookup();
    }

    @Test
    void terrainTilesAtTheSeamForNoiseGeneratorsAndNotForAForeignOne() {
        assertTrue(GeneratorShapes.isTilingAtSeam(noiseGenerator(worldgen)));
        assertTrue(GeneratorShapes.isTilingAtSeam(noiseSubclassGenerator(worldgen)));
        assertFalse(GeneratorShapes.isTilingAtSeam(foreignGenerator()));
    }

    @Test
    void aNoiseSubclassOverworldKeepsItsClassAndTakesTheShape() {
        ChunkGenerator overworld = noiseSubclassGenerator(worldgen);
        WorldDimensions dimensions = overworldOnly(overworld);
        FlatShape shape = squareTorus(CHOSEN_WIDTH_CHUNKS);

        WorldDimensions shaped = ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD,
                new CarriedShape(null, shape, ShapeOptions.DEFAULT));

        assertNotSame(dimensions, shaped, "the shaped dimensions are the object the apply guard compares");
        assertEquals(shape, ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD));
        assertSame(overworld, shaped.get(LevelStem.OVERWORLD).orElseThrow().generator());
    }

    @Test
    void aForeignOverworldKeepsItsClassAndTakesTheShape() {
        ChunkGenerator overworld = foreignGenerator();
        WorldDimensions dimensions = overworldOnly(overworld);
        FlatShape shape = squareTorus(CHOSEN_WIDTH_CHUNKS);

        WorldDimensions shaped = ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD,
                new CarriedShape(null, shape, ShapeOptions.DEFAULT));

        assertNotSame(dimensions, shaped, "the shaped dimensions are the object the apply guard compares");
        assertEquals(shape, ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD));
        assertSame(overworld, shaped.get(LevelStem.OVERWORLD).orElseThrow().generator());
    }

    @Test
    void aTorusReachesEveryStemOverANoiseSubclassOverworld() {
        WorldDimensions shaped = LayoutDimensions.apply(vanillaThree(noiseSubclassGenerator(worldgen)),
                LayoutFixtures.defaultTorus());

        assertNotNull(ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD), "the overworld carries no shape");
        assertNotNull(ShapedDimensions.getShape(shaped, LevelStem.NETHER), "the nether was never reached");
        assertNotNull(ShapedDimensions.getShape(shaped, LevelStem.END), "the End was never reached");
    }

    @Test
    void aTorusCarriesItsClimateChoiceIntoEveryStemAndReadsItBack() {
        WorldDimensions shaped = LayoutDimensions.apply(vanillaThree(noiseSubclassGenerator(worldgen)),
                LayoutFixtures.defaultTorus());

        assertNotNull(ShapedDimensions.getShape(shaped, LevelStem.NETHER), "the nether was never reached");
        for (ResourceKey<LevelStem> key : List.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END)) {
            CarriedShape carried = ShapedDimensions.getCarriedShape(shaped, key);
            assertNotNull(carried, key.identifier().toString());
            assertEquals(LayoutFixtures.PLAIN.get(CompactBiomes.FEATURE.getOption()),
                    carried.getOptions().get(CompactBiomes.FEATURE.getOption()), key.identifier().toString());
        }
        WorldLayout read = LayoutDimensions.read(shaped);
        assertNotNull(read, "the shaped world does not read back as a layout");
        assertEquals(LayoutFixtures.PLAIN.get(CompactBiomes.FEATURE.getOption()),
                read.overworld().options().get(CompactBiomes.FEATURE.getOption()));
    }

    @Test
    void strippingClearsAShapeStampedByAnEarlierAttempt() {
        ChunkGenerator overworld = noiseSubclassGenerator(worldgen);
        WorldDimensions shaped = ShapedDimensions.withShape(overworldOnly(overworld), LevelStem.OVERWORLD,
                new CarriedShape(null, squareTorus(CHOSEN_WIDTH_CHUNKS), ShapeOptions.DEFAULT));

        ShapedDimensions.stripShapes(shaped);

        assertNull(ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD));
    }

    @Test
    void aVanillaNoiseOverworldTakesTheShapeOnACopyAndLeavesThePresetsOwnGeneratorUnshaped() {
        NoiseBasedChunkGenerator overworld = (NoiseBasedChunkGenerator) noiseGenerator(worldgen);
        FlatShape shape = squareTorus(CHOSEN_WIDTH_CHUNKS);

        WorldDimensions shaped = ShapedDimensions.withShape(overworldOnly(overworld), LevelStem.OVERWORLD,
                new CarriedShape(null, shape, ShapeOptions.DEFAULT));

        NoiseBasedChunkGenerator copy = (NoiseBasedChunkGenerator) overworldGeneratorOf(shaped);
        assertNotSame(overworld, copy);
        assertEquals(NoiseBasedChunkGenerator.class, copy.getClass());
        assertSame(overworld.getBiomeSource(), copy.getBiomeSource());
        assertSame(overworld.generatorSettings(), copy.generatorSettings());
        assertEquals(shape, ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD));
        assertNull(GeneratorShapes.getCarriedShape(overworld));
    }

    @Test
    void aSuperflatOverworldTakesTheShapeOnACopyOfItsOwnSettings() {
        FlatLevelSource overworld = flatGenerator();
        FlatShape shape = squareTorus(CHOSEN_WIDTH_CHUNKS);

        WorldDimensions shaped = ShapedDimensions.withShape(overworldOnly(overworld), LevelStem.OVERWORLD,
                new CarriedShape(null, shape, ShapeOptions.DEFAULT));

        FlatLevelSource copy = (FlatLevelSource) overworldGeneratorOf(shaped);
        assertNotSame(overworld, copy);
        assertEquals(FlatLevelSource.class, copy.getClass());
        assertSame(overworld.settings(), copy.settings());
        assertEquals(shape, ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD));
        assertNull(GeneratorShapes.getCarriedShape(overworld));
    }

    @Test
    void reShapingASuperflatWorldTakesTheNewShapeOnItsOwnSettings() {
        FlatLevelSource overworld = flatGenerator();
        FlatShape cylinder = ShapeFixtures.cylinder(Direction.Axis.X, CHOSEN_WIDTH_CHUNKS);
        WorldDimensions torus = ShapedDimensions.withShape(overworldOnly(overworld), LevelStem.OVERWORLD,
                new CarriedShape(null, squareTorus(CHOSEN_WIDTH_CHUNKS), ShapeOptions.DEFAULT));

        WorldDimensions reshaped = ShapedDimensions.withShape(torus, LevelStem.OVERWORLD,
                new CarriedShape(null, cylinder, ShapeOptions.DEFAULT));

        assertEquals(cylinder, ShapedDimensions.getShape(reshaped, LevelStem.OVERWORLD));
        assertSame(overworld.settings(), ((FlatLevelSource) overworldGeneratorOf(reshaped)).settings());
    }

    @Test
    void strippingAVanillaWorldLeavesItsGeneratorsPlain() {
        WorldDimensions shaped = LayoutDimensions.apply(vanillaThree(noiseGenerator(worldgen)),
                LayoutFixtures.defaultTorus());

        ShapedDimensions.stripShapes(shaped);

        for (ResourceKey<LevelStem> key : List.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END)) {
            assertNull(ShapedDimensions.getShape(shaped, key), key.identifier().toString());
        }
        assertEquals(NoiseBasedChunkGenerator.class, overworldGeneratorOf(shaped).getClass());
    }

    @Test
    void aShapedChosenPresetReplacesAnUnshapedResult() {
        WorldDimensions chosen = LayoutDimensions.apply(vanillaThree(noiseGenerator(worldgen)),
                LayoutFixtures.defaultTorus());
        WorldDimensions replaced = overworldOnly(noiseGenerator(worldgen));

        assertSame(chosen, ShapedDimensions.keepChosenShape(replaced, chosen));
    }

    @Test
    void aShapedResultStandsOverAShapedChosenPreset() {
        WorldDimensions chosen = LayoutDimensions.apply(vanillaThree(noiseGenerator(worldgen)),
                LayoutFixtures.defaultTorus());
        WorldDimensions created = LayoutDimensions.apply(vanillaThree(noiseGenerator(worldgen)),
                LayoutFixtures.defaultCylinder());

        assertSame(created, ShapedDimensions.keepChosenShape(created, chosen));
    }

    private static ChunkGenerator overworldGeneratorOf(WorldDimensions dimensions) {
        return dimensions.get(LevelStem.OVERWORLD).orElseThrow().generator();
    }

    private static FlatLevelSource flatGenerator() {
        return new FlatLevelSource(new FlatLevelGeneratorSettings(Optional.empty(),
                worldgen.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS), List.of()));
    }

    private static WorldDimensions overworldOnly(ChunkGenerator overworld) {
        return selected(Map.of(LevelStem.OVERWORLD, stem(OVERWORLD_SCALE, overworld)));
    }

    private static WorldDimensions vanillaThree(ChunkGenerator overworld) {
        return selected(Map.of(
                LevelStem.OVERWORLD, stem(OVERWORLD_SCALE, overworld),
                LevelStem.NETHER, stem(NETHER_COORDINATE_SCALE, noiseGenerator(worldgen)),
                LevelStem.END, stem(OVERWORLD_SCALE, noiseGenerator(worldgen))));
    }
}
