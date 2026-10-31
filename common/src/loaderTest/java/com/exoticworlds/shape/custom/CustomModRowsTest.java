package com.exoticworlds.shape.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.engine.gen.ShapedDimensions;
import com.exoticworlds.shape.WorldOptionSetup;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.cylinder.CylinderDimensions;
import com.exoticworlds.shape.torus.TorusDimensions;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
class CustomModRowsTest {
    private static final String TEST_NAMESPACE = "exotic_worlds_test";

    private static final ModDimension SKY = new ModDimension(stemKey("sky"));
    private static final ModDimension MINE = new ModDimension(stemKey("mine"));

    private static final DimensionShape NONE = new DimensionShape(null, CustomSettings.DEFAULT_OPTIONS);

    private static HolderLookup.Provider worldgen;
    private static GenerationOptions compact6;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        worldgen = VanillaRegistries.createLookup();
        compact6 = CustomSettings.DEFAULT_OPTIONS.with(CompactBiomes.OPTION, ClimateScale.custom(6));
    }

    @Test
    void modRowsBesideANormalWorldRoundTrip() {
        CustomSettings chosen = new CustomSettings(NONE, NONE, NONE)
                .with(SKY, new DimensionShape(LoopSpans.ofWidths(48, 32), compact6))
                .with(MINE, NONE);

        WorldDimensions created = CustomDimensions.apply(vanillaDimensions(), chosen);

        assertNull(ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertEquals(2, ShapedDimensions.dimensionTableOf(created).entries().size());
        assertEquals(chosen, CustomDimensions.read(created));
    }

    @Test
    void modRowsBesideAWrappedOverworldRoundTrip() {
        CustomSettings chosen = new CustomSettings(
                new DimensionShape(LoopSpans.ofWidth(64), CustomSettings.DEFAULT_OPTIONS), NONE, NONE)
                .with(SKY, new DimensionShape(LoopSpans.ofWidth(Direction.Axis.Z, 40), compact6));

        assertEquals(chosen, CustomDimensions.read(CustomDimensions.apply(vanillaDimensions(), chosen)));
    }

    @Test
    void aRowLeftFromOverworldWritesNoEntry() {
        CustomSettings chosen = new CustomSettings(NONE, NONE, NONE).with(SKY, NONE).with(SKY, null);

        WorldDimensions created = CustomDimensions.apply(vanillaDimensions(), chosen);

        assertEquals(0, ShapedDimensions.dimensionTableOf(created).entries().size());
        assertNull(CustomDimensions.read(created), "a world no row of which wraps read back as Custom");
    }

    @Test
    void torusAndCylinderLeaveAWorldWithModRowsToCustom() {
        CustomSettings torus = new CustomSettings(
                new DimensionShape(LoopSpans.ofWidth(64), CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidth(16), CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidth(256), CustomSettings.DEFAULT_OPTIONS));
        CustomSettings cylinder = new CustomSettings(
                new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 64), CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 16), CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 256), CustomSettings.DEFAULT_OPTIONS));
        assertNotNull(TorusDimensions.read(CustomDimensions.apply(vanillaDimensions(), torus)),
                "the torus world does not read as a torus even without rows, so this run says nothing");
        assertNotNull(CylinderDimensions.read(CustomDimensions.apply(vanillaDimensions(), cylinder)),
                "the cylinder world does not read as a cylinder even without rows, so this run says nothing");

        WorldDimensions torusWorld = CustomDimensions.apply(vanillaDimensions(), torus.with(SKY, NONE));
        WorldDimensions cylinderWorld = CustomDimensions.apply(vanillaDimensions(), cylinder.with(SKY, NONE));

        assertNull(TorusDimensions.read(torusWorld));
        assertNull(CylinderDimensions.read(cylinderWorld));
        assertEquals(torus.with(SKY, NONE), CustomDimensions.read(torusWorld));
        assertEquals(cylinder.with(SKY, NONE), CustomDimensions.read(cylinderWorld));
    }

    @Test
    void strippingClearsTheTableAnEarlierApplyWrote() {
        WorldDimensions created = CustomDimensions.apply(vanillaDimensions(),
                new CustomSettings(NONE, NONE, NONE).with(SKY, NONE));

        assertEquals(0, ShapedDimensions.dimensionTableOf(ShapedDimensions.stripShapes(created)).entries().size());
    }

    private static ResourceKey<LevelStem> stemKey(String path) {
        return ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(TEST_NAMESPACE, path));
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
