package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.engine.gen.ShapedDimensions;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

@Timeout(60)
class LayoutModRowsTest {
    private static final String TEST_NAMESPACE = "exotic_worlds_test";

    private static final ResourceKey<LevelStem> SKY = stemKey("sky");
    private static final ResourceKey<LevelStem> MINE = stemKey("mine");

    private static final ShapeOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;
    private static final DimensionLayout NORMAL = DimensionLayout.normal(PLAIN);

    private static HolderLookup.Provider worldgen;
    private static ShapeOptions compact6;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createLookup();
        compact6 = PLAIN.with(CompactBiomes.OPTION, ClimateScale.custom(6));
    }

    @Test
    void modRowsBesideANormalWorldRoundTrip() {
        WorldLayout chosen = new WorldLayout(NORMAL, NORMAL, NORMAL, Map.of(
                SKY, DimensionLayout.looped(ShapeFixtures.torus(48, 32), compact6),
                MINE, NORMAL), NetherScales.DEFAULT);

        WorldDimensions created = LayoutDimensions.apply(vanillaDimensions(), chosen);

        assertNull(ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
        assertEquals(2, ShapedDimensions.getDimensionTable(created).getEntries().size());
        assertEquals(chosen, LayoutDimensions.read(created));
    }

    @Test
    void modRowsBesideAWrappedOverworldRoundTrip() {
        WorldLayout chosen = new WorldLayout(DimensionLayout.looped(ShapeFixtures.torus(64), PLAIN), NORMAL, NORMAL,
                Map.of(SKY, DimensionLayout.looped(ShapeFixtures.cylinder(Direction.Axis.Z, 40), compact6)),
                NetherScales.DEFAULT);

        assertEquals(chosen, LayoutDimensions.read(LayoutDimensions.apply(vanillaDimensions(), chosen)));
    }

    private static ResourceKey<LevelStem> stemKey(String path) {
        return ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath(TEST_NAMESPACE, path));
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
                new FixedBiomeSource(worldgen.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                worldgen.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(noise));
        return new LevelStem(worldgen.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(type), generator);
    }
}
