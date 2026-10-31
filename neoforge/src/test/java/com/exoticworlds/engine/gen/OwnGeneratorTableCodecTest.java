package com.exoticworlds.engine.gen;

import static com.exoticworlds.engine.gen.BakeStampFixture.noiseGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.selected;
import static com.exoticworlds.engine.gen.BakeStampFixture.squareTorus;
import static com.exoticworlds.engine.gen.BakeStampFixture.stem;
import static com.exoticworlds.engine.gen.BakeStampFixture.stemKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.shape.WorldOptionSetup;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

@Timeout(60)
class OwnGeneratorTableCodecTest {
    private static final double OVERWORLD_SCALE = 1.0;
    private static final int OVERWORLD_CHUNK_WIDTH = 64;

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        worldgen = VanillaRegistries.createWorldLookup();
    }

    @Test
    void aDimensionTableOnOurOwnGeneratorSurvivesTheRoundTrip() {
        CarriedShape overworldShape = new CarriedShape(squareTorus(OVERWORLD_CHUNK_WIDTH), GenerationOptions.DEFAULT);
        ChunkGenerator generator = ShapedDimensions.withShape(
                selected(Map.of(LevelStem.OVERWORLD, stem(OVERWORLD_SCALE, noiseGenerator(worldgen)))),
                LevelStem.OVERWORLD, overworldShape)
                .get(LevelStem.OVERWORLD).orElseThrow().generator();
        assertInstanceOf(LoopedChunkGenerator.class, generator);
        DimensionTable table = DimensionTable.of(Map.of(stemKey("unwrapped"), DimensionTable.Entry.UNWRAPPED));
        ((ShapeStamp) generator).exotic_worlds$stampDimensionTable(table);

        RegistryOps<Tag> ops = worldgen.createSerializationContext(NbtOps.INSTANCE);
        ChunkGenerator decoded = ChunkGenerator.CODEC.parse(ops,
                ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow()).getOrThrow();

        assertEquals(table, ShapedDimensions.dimensionTableOf(decoded));
        assertEquals(overworldShape, ShapedChunkGenerator.carriedShapeOf(decoded));
    }
}
