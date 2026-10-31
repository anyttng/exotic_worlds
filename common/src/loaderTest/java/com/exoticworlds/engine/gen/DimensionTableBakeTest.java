package com.exoticworlds.engine.gen;

import static com.exoticworlds.engine.gen.BakeStampFixture.OVERWORLD_CHUNK_WIDTH;
import static com.exoticworlds.engine.gen.BakeStampFixture.chunkWidth;
import static com.exoticworlds.engine.gen.BakeStampFixture.datapackRegistry;
import static com.exoticworlds.engine.gen.BakeStampFixture.foreignGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.noiseGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.selected;
import static com.exoticworlds.engine.gen.BakeStampFixture.shapeOf;
import static com.exoticworlds.engine.gen.BakeStampFixture.shapedGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.squareTorus;
import static com.exoticworlds.engine.gen.BakeStampFixture.stem;
import static com.exoticworlds.engine.gen.BakeStampFixture.stemKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.shape.WorldOptionSetup;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

@Timeout(60)
class DimensionTableBakeTest {
    private static final double SAME_SCALE = 1.0;

    private static final FlatShape TABLE_SHAPE = ShapeFixtures.torus(WorldLoopBounds.ofWidths(48, 32));

    private static final ResourceKey<LevelStem> LISTED = stemKey("listed");
    private static final ResourceKey<LevelStem> UNLISTED = stemKey("unlisted");

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        worldgen = VanillaRegistries.createWorldLookup();
    }

    @Test
    void aWrappedEntryShapesItsStemBesideAnUnwrappedOverworld() {
        Registry<LevelStem> baked = bake(unwrappedOverworld(wrappedTable()), datapackRegistry(Map.of(
                LISTED, stem(SAME_SCALE, foreignGenerator()),
                UNLISTED, stem(SAME_SCALE, foreignGenerator()))));

        assertEquals(TABLE_SHAPE, shapeOf(baked, LISTED));
        assertNull(shapeOf(baked, UNLISTED), "an unlisted stem derived a shape from an unwrapped overworld");
    }

    @Test
    void anUnwrappedEntryLeavesItsStemUnwrappedWhileAnUnlistedOneDerives() {
        DimensionTable table = DimensionTable.of(Map.of(LISTED, DimensionTable.Entry.UNWRAPPED));
        Registry<LevelStem> baked = bake(wrappedOverworld(table), datapackRegistry(Map.of(
                LISTED, stem(SAME_SCALE, foreignGenerator()),
                UNLISTED, stem(SAME_SCALE, foreignGenerator()))));

        assertNull(shapeOf(baked, LISTED));
        FlatShape derived = shapeOf(baked, UNLISTED);
        assertNotNull(derived, "the unlisted stem derived nothing, so this run says nothing about the entry");
        assertEquals(OVERWORLD_CHUNK_WIDTH, chunkWidth(derived, Direction.Axis.X));
    }

    @Test
    void aStemCarryingItsOwnShapeOutranksItsEntry() {
        FlatShape own = squareTorus(OVERWORLD_CHUNK_WIDTH);
        Registry<LevelStem> baked = bake(unwrappedOverworld(wrappedTable()), datapackRegistry(Map.of(
                LISTED, stem(SAME_SCALE, shapedGenerator(own)))));

        assertEquals(own, shapeOf(baked, LISTED));
    }

    @Test
    void aSecondBakeOverTheSameDatapackStemsTakesTheNewTable() {
        Registry<LevelStem> datapack = datapackRegistry(Map.of(LISTED, stem(SAME_SCALE, foreignGenerator())));
        assertEquals(TABLE_SHAPE, shapeOf(bake(unwrappedOverworld(wrappedTable()), datapack), LISTED),
                "the first bake shaped nothing, so this run says nothing about the second");

        DimensionTable unwrapped = DimensionTable.of(Map.of(LISTED, DimensionTable.Entry.UNWRAPPED));
        assertNull(shapeOf(bake(wrappedOverworld(unwrapped), datapack), LISTED));
    }

    @Test
    void aSecondBakeOverTheSameDatapackStemsDropsADerivationTheOverworldNoLongerGives() {
        Registry<LevelStem> datapack = datapackRegistry(Map.of(UNLISTED, stem(SAME_SCALE, foreignGenerator())));
        assertNotNull(shapeOf(bake(wrappedOverworld(DimensionTable.EMPTY), datapack), UNLISTED),
                "the first bake derived nothing, so this run says nothing about the second");

        assertNull(shapeOf(bake(unwrappedOverworld(DimensionTable.EMPTY), datapack), UNLISTED));
    }

    @Test
    void aTableNamingAVanillaDimensionIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> DimensionTable.of(Map.of(LevelStem.END, DimensionTable.Entry.UNWRAPPED)));
    }

    private static DimensionTable wrappedTable() {
        return DimensionTable.of(Map.of(LISTED,
                new DimensionTable.Entry(new CarriedShape(TABLE_SHAPE, GenerationOptions.DEFAULT))));
    }

    private static WorldDimensions unwrappedOverworld(DimensionTable table) {
        return withTable(noiseGenerator(worldgen), table);
    }

    private static WorldDimensions wrappedOverworld(DimensionTable table) {
        return withTable(shapedGenerator(squareTorus(OVERWORLD_CHUNK_WIDTH)), table);
    }

    private static WorldDimensions withTable(ChunkGenerator overworld, DimensionTable table) {
        ((ShapeStamp) overworld).exotic_worlds$stampDimensionTable(table);
        return selected(Map.of(LevelStem.OVERWORLD, stem(SAME_SCALE, overworld)));
    }

    private static Registry<LevelStem> bake(WorldDimensions stored, Registry<LevelStem> datapack) {
        return stored.bake(datapack).dimensions();
    }
}
