package com.exoticworlds.engine.gen;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.gen.ShapedChunkGenerator;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.climate.CompactBiomes;
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;


import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

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
        assertTrue(ShapedChunkGenerator.isTilingAtSeam(noiseGenerator(worldgen)));
        assertTrue(ShapedChunkGenerator.isTilingAtSeam(noiseSubclassGenerator(worldgen)));
        assertFalse(ShapedChunkGenerator.isTilingAtSeam(foreignGenerator()));
    }

    @Test
    void aNoiseSubclassOverworldKeepsItsClassAndTakesTheShape() {
        ChunkGenerator overworld = noiseSubclassGenerator(worldgen);
        WorldDimensions dimensions = overworldOnly(overworld);
        FlatShape shape = squareTorus(CHOSEN_WIDTH_CHUNKS);

        WorldDimensions shaped = ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD,
                new CarriedShape(shape, ShapeOptions.DEFAULT));

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
                new CarriedShape(shape, ShapeOptions.DEFAULT));

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
            assertEquals(LayoutFixtures.PLAIN.get(CompactBiomes.OPTION),
                    carried.getOptions().get(CompactBiomes.OPTION), key.identifier().toString());
        }
        WorldLayout read = LayoutDimensions.read(shaped);
        assertNotNull(read, "the shaped world does not read back as a layout");
        assertEquals(LayoutFixtures.PLAIN.get(CompactBiomes.OPTION),
                read.overworld().options().get(CompactBiomes.OPTION));
    }

    @Test
    void strippingClearsAShapeStampedByAnEarlierAttempt() {
        ChunkGenerator overworld = noiseSubclassGenerator(worldgen);
        WorldDimensions shaped = ShapedDimensions.withShape(overworldOnly(overworld), LevelStem.OVERWORLD,
                new CarriedShape(squareTorus(CHOSEN_WIDTH_CHUNKS), ShapeOptions.DEFAULT));

        assertNull(ShapedDimensions.getShape(ShapedDimensions.stripShapes(shaped), LevelStem.OVERWORLD));
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
