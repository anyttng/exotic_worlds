package com.exoticworlds.shape.layout.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.layout.DimensionLayout;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class StemFactsTest {
    private static final ResourceKey<LevelStem> ISLANDS =
            ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath("example_mod", "islands"));

    private static final ResourceKey<LevelStem> PLAINS =
            ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath("example_mod", "plains"));

    private static final int OVERWORLD_WIDTH_CHUNKS = 64;

    private static final double SAME_SCALE = 1.0;

    private static final StemFacts FACTS = new StemFacts(
            Map.of(LevelStem.OVERWORLD, SAME_SCALE, ISLANDS, SAME_SCALE, PLAINS, SAME_SCALE), Set.of(ISLANDS));

    @BeforeAll
    static void registerShapes() {
        StartupRegistrations.options();
    }

    @Test
    void aStemListedWithEndTerrainAndTheEndItselfAreEndTerrain() {
        assertTrue(FACTS.terrainOf(ISLANDS).isEndTerrain());
        assertTrue(FACTS.terrainOf(LevelStem.END).isEndTerrain());
        assertFalse(FACTS.terrainOf(PLAINS).isEndTerrain());
    }

    @Test
    void aModDimensionWithEndTerrainDerivesByTheEndRule() {
        int endWidthChunks = WorldLoopSizes.endWidthChunksFor(OVERWORLD_WIDTH_CHUNKS);
        DimensionLayout overworld = DimensionLayout.shaped(DeclaredShapes.TORUS,
                ShapeFixtures.torus(OVERWORLD_WIDTH_CHUNKS), ShapeOptions.DEFAULT);

        assertEquals(FlatShape.unskewed(WorldLoopBounds.ofWidths(endWidthChunks, endWidthChunks)),
                FACTS.modDimension(ISLANDS, overworld));
        assertEquals(ShapeFixtures.torus(OVERWORLD_WIDTH_CHUNKS), FACTS.modDimension(PLAINS, overworld));
    }
}
