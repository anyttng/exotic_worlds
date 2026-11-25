package com.exoticworlds.engine.gen;

import static com.exoticworlds.engine.gen.BakeStampFixture.noiseGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.selected;
import static com.exoticworlds.engine.gen.BakeStampFixture.squareTorus;
import static com.exoticworlds.engine.gen.BakeStampFixture.stem;
import static com.exoticworlds.engine.gen.BakeStampFixture.stemKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.shape.StartupRegistrations;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

@Timeout(60)
class ShapedGeneratorTableCodecTest {
    private static final double OVERWORLD_SCALE = 1.0;
    private static final int OVERWORLD_WIDTH_CHUNKS = 64;

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aDimensionTableOnAShapedVanillaGeneratorSurvivesTheRoundTrip() {
        CarriedShape overworldShape = new CarriedShape(null, squareTorus(OVERWORLD_WIDTH_CHUNKS), ShapeOptions.DEFAULT);
        ChunkGenerator generator = ShapedDimensions.withShape(
                selected(Map.of(LevelStem.OVERWORLD, stem(OVERWORLD_SCALE, noiseGenerator(worldgen)))),
                LevelStem.OVERWORLD, overworldShape)
                .get(LevelStem.OVERWORLD).orElseThrow().generator();
        assertEquals(NoiseBasedChunkGenerator.class, generator.getClass());
        DimensionTable table = DimensionTable.of(Map.of(stemKey("unwrapped"), DimensionTable.Entry.UNWRAPPED));
        ((ShapeStamp) generator).exotic_worlds$stampDimensionTable(table);

        RegistryOps<Tag> ops = worldgen.createSerializationContext(NbtOps.INSTANCE);
        ChunkGenerator decoded = ChunkGenerator.CODEC.parse(ops,
                ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow()).getOrThrow();

        assertEquals(table, ShapedDimensions.getDimensionTable(decoded));
        assertEquals(overworldShape, GeneratorShapes.getCarriedShape(decoded));
    }
}
