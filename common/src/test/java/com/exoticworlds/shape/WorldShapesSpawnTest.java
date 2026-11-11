package com.exoticworlds.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.shape.ShapeModule;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.shape.custom.CustomSettings;
import com.exoticworlds.shape.custom.CustomShape;
import com.exoticworlds.shape.custom.DimensionShape;
import com.exoticworlds.shape.custom.VanillaDimension;
import com.exoticworlds.shape.cylinder.CylinderShape;
import com.exoticworlds.shape.torus.TorusShape;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class WorldShapesSpawnTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createLookup();

    private static final SpawnPoint PICKED = new SpawnPoint(3, -5);

    @BeforeAll
    static void registerTheModsShapes() {
        TorusShape.register(false);
        CylinderShape.register(false);
        CustomShape.register(false);
    }

    @AfterEach
    void leaveTheShapesAsTheScreenOpensThem() {
        WorldShapes.resetToDefault();
    }

    @Test
    void theTorusCreatesItsOverworldAtTheMovedSpawnPoint() {
        assertSpawnMovedInto(TorusShape.MODULE);
    }

    @Test
    void theCylinderCreatesItsOverworldAtTheMovedSpawnPoint() {
        assertSpawnMovedInto(CylinderShape.MODULE);
    }

    @Test
    void customCreatesItsOverworldAtTheMovedSpawnPoint() {
        assertSpawnMovedInto(CustomShape.MODULE);
    }

    @Test
    void customWithAPlainOverworldRefusesTheSpawnPoint() {
        CustomSettings plainOverworld = CustomSettings.DEFAULT.with(VanillaDimension.OVERWORLD,
                new DimensionShape(null, CustomSettings.DEFAULT_OPTIONS));
        CustomShape.MODULE.settings(plainOverworld);
        WorldShapes.select(registered(CustomShape.MODULE.id()));

        assertFalse(WorldShapes.moveSpawn(PICKED));
        assertEquals(plainOverworld, CustomShape.MODULE.settings());
    }

    @Test
    void normalRefusesTheSpawnPoint() {
        WorldShapes.select(WorldShapes.NORMAL);

        assertFalse(WorldShapes.moveSpawn(PICKED));
    }

    private static void assertSpawnMovedInto(ShapeModule<?> module) {
        WorldShapes.select(registered(module.id()));

        assertTrue(WorldShapes.moveSpawn(PICKED));

        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, vanillaDimensions());
        CarriedShape overworld = ShapedChunkGenerator.carriedShapeOf(
                created.dimensions().get(LevelStem.OVERWORLD).generator());
        assertNotNull(overworld);
        assertEquals(PICKED, overworld.generationOptions().get(SpawnPoint.OPTION));
    }

    private static WorldShape registered(ResourceLocation id) {
        return WorldShapes.shapes().stream()
                .filter(shape -> shape.id().equals(id))
                .findFirst()
                .orElseThrow();
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
