package com.exoticworlds.engine.gen;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.migration.FormerNamespace;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.shape.WorldOptionSetup;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import static com.exoticworlds.engine.gen.BakeStampFixture.noiseGenerator;
import static com.exoticworlds.engine.gen.BakeStampFixture.squareTorus;
import static com.exoticworlds.engine.gen.BakeStampFixture.stamped;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.mojang.serialization.DataResult;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

@Timeout(60)
class StampedGeneratorCodecTest {
    private static final String KEY_PREFIX = ExoticWorlds.MODID + ":";
    private static final String SHAPE_KEY = KEY_PREFIX + CarriedShape.WRAPPING_KEY;
    private static final String CLIMATE_SCALE_KEY =
            ExoticWorlds.MODID + ":" + CompactBiomes.KEY;

    private static final GenerationOptions UNCOMPRESSED =
            GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF);

    private static final int STAMPED_CHUNK_WIDTH = 64;

    private static final String UNREADABLE_SHAPE = "not a shape at all";

    private static final String TABLE_KEY = KEY_PREFIX + DimensionTable.KEY;

    private static final DimensionTable TABLE = DimensionTable.of(Map.of(
            BakeStampFixture.stemKey("wrapped"), new DimensionTable.Entry(new CarriedShape(
                    ShapeFixtures.torus(WorldLoopBounds.ofWidths(48, 32)), UNCOMPRESSED)),
            BakeStampFixture.stemKey("unwrapped"), DimensionTable.Entry.UNWRAPPED));

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aStampedShapeSurvivesTheRoundTrip() {
        FlatShape shape = squareTorus(STAMPED_CHUNK_WIDTH);
        CompoundTag encoded = encode(stamped(noiseGenerator(worldgen), shape));

        assertTrue(encoded.contains(SHAPE_KEY), "the stamped shape never reached the encoded generator");
        assertFalse(encoded.contains(CLIMATE_SCALE_KEY), "a compressed stamp wrote the choice it need not");

        CarriedShape decoded = carriedShapeOf(decode(encoded).getOrThrow());
        assertEquals(shape, decoded.shape());
        assertEquals(ClimateScale.AUTO, decoded.generationOptions().get(CompactBiomes.OPTION));
    }

    @Test
    void aStampedChoiceAgainstCompressionSurvivesTheRoundTrip() {
        FlatShape shape = squareTorus(STAMPED_CHUNK_WIDTH);
        CompoundTag encoded = encode(stamped(noiseGenerator(worldgen), shape, UNCOMPRESSED));

        assertTrue(encoded.contains(CLIMATE_SCALE_KEY), "the stamped choice never reached the encoded generator");

        CarriedShape decoded = carriedShapeOf(decode(encoded).getOrThrow());
        assertEquals(shape, decoded.shape());
        assertEquals(ClimateScale.OFF, decoded.generationOptions().get(CompactBiomes.OPTION));
    }

    @Test
    void aStampUnderTheFormerNamespaceIsReadAndWrittenBackUnderTheCurrentOne() {
        FlatShape shape = squareTorus(STAMPED_CHUNK_WIDTH);
        CompoundTag current = encode(stamped(noiseGenerator(worldgen), shape, UNCOMPRESSED));
        CompoundTag former = new CompoundTag();
        current.forEach((key, value) -> former.put(
                key.startsWith(KEY_PREFIX) ? FormerNamespace.formerKey(key) : key, value.copy()));
        assertTrue(former.contains(FormerNamespace.formerKey(SHAPE_KEY)), "the fixture holds no former stamp");

        ChunkGenerator decoded = decode(former).getOrThrow();
        CarriedShape carried = carriedShapeOf(decoded);
        assertEquals(shape, carried.shape());
        assertEquals(ClimateScale.OFF, carried.generationOptions().get(CompactBiomes.OPTION));

        CompoundTag rewritten = encode(decoded);
        assertTrue(rewritten.contains(SHAPE_KEY));
        assertFalse(rewritten.toString().contains(FormerNamespace.NAMESPACE), rewritten.toString());
    }

    @Test
    void anUnstampedGeneratorCarriesNoShapeKey() {
        CompoundTag encoded = encode(noiseGenerator(worldgen));

        assertFalse(encoded.contains(SHAPE_KEY));
        assertNull(ShapedChunkGenerator.carriedShapeOf(decode(encoded).getOrThrow()));
    }

    @Test
    void aDimensionTableOnAVanillaGeneratorSurvivesTheRoundTrip() {
        ChunkGenerator generator = noiseGenerator(worldgen);
        ((ShapeStamp) generator).exotic_worlds$stampDimensionTable(TABLE);
        CompoundTag encoded = encode(generator);

        assertTrue(encoded.contains(TABLE_KEY), "the table never reached the encoded generator");
        assertFalse(encoded.contains(SHAPE_KEY), "a table alone wrote a shape");
        assertEquals(TABLE, ShapedDimensions.dimensionTableOf(decode(encoded).getOrThrow()));
    }

    @Test
    void aDimensionTableNamingAVanillaDimensionRefusesTheGenerator() {
        CompoundTag encoded = encode(noiseGenerator(worldgen));
        CompoundTag table = new CompoundTag();
        table.put(LevelStem.NETHER.identifier().toString(), new CompoundTag());
        encoded.put(TABLE_KEY, table);

        assertTrue(decode(encoded).isError(), "a table naming the nether loaded");
    }

    @Test
    void anUnreadableStoredShapeRefusesTheGenerator() {
        CompoundTag encoded = encode(noiseGenerator(worldgen));
        encoded.putString(SHAPE_KEY, UNREADABLE_SHAPE);

        assertTrue(decode(encoded).isError(), "an unreadable stored shape loaded as an ordinary generator");
    }

    private static CarriedShape carriedShapeOf(ChunkGenerator generator) {
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(generator);
        assertNotNull(carried, "the decoded generator carries no shape");
        return carried;
    }

    private static CompoundTag encode(ChunkGenerator generator) {
        return assertInstanceOf(CompoundTag.class,
                ChunkGenerator.CODEC.encodeStart(ops(), generator).getOrThrow());
    }

    private static DataResult<ChunkGenerator> decode(CompoundTag encoded) {
        return ChunkGenerator.CODEC.parse(ops(), encoded);
    }

    private static RegistryOps<Tag> ops() {
        return worldgen.createSerializationContext(NbtOps.INSTANCE);
    }
}
