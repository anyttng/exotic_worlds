package com.exoticworlds.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;
import com.exoticworlds.engine.net.WrappingSettingsPayload;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class ShapeRegistrationBoundaryTest {
    private static RegistryAccess.Frozen registries;

    private static HolderLookup.Provider worldgen;

    private static final Identifier CYLINDER_ID =
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "boundary_test_cylinder");

    private static final FlatShape CYLINDER = ShapeFixtures.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE));

    private static final FlatShape WIDER_CYLINDER = ShapeFixtures.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-64, 64), AxisBounds.Unbounded.INSTANCE));

    private static WorldShape cylinder;

    private static boolean settingsWereReset;

    @BeforeAll
    static void registerTheShapeThroughThePublicSeamsAlone() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        worldgen = VanillaRegistries.createLookup();
        cylinder = WorldShape.of(
                CYLINDER_ID,
                (registries, dimensions) -> ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD,
                        new CarriedShape(null, CYLINDER, ShapeOptions.DEFAULT)),
                () -> settingsWereReset = true,
                (registries, dimensions) ->
                        CYLINDER.equals(ShapedDimensions.getShape(dimensions, LevelStem.OVERWORLD)));

        WorldShapes.register(cylinder);
        ShapeCustomizers.register(CYLINDER_ID, parent -> parent);
    }

    @AfterEach
    void leaveTheRegistryAsItWasFound() {
        WorldShapes.select(WorldShapes.NORMAL);
        settingsWereReset = false;
    }

    private static WorldDimensions vanillaOverworld() {
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(worldgen.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                worldgen.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD));

        return new WorldDimensions(Map.of(LevelStem.OVERWORLD, new LevelStem(
                worldgen.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                generator)));
    }

    private static WorldDimensions vanillaOverworldAndNether() {
        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>(vanillaOverworld().dimensions());
        stems.put(LevelStem.NETHER, new LevelStem(
                worldgen.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.NETHER),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(worldgen.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.NETHER_WASTES)),
                        worldgen.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.NETHER))));
        return new WorldDimensions(stems);
    }

    private static NoiseBasedChunkGenerator overworldGenerator(WorldDimensions dimensions) {
        return (NoiseBasedChunkGenerator) dimensions.dimensions().get(LevelStem.OVERWORLD).generator();
    }

    @Test
    void theShapeIsOfferedAfterNormal() {
        List<WorldShape> offered = WorldShapes.getShapes();

        assertSame(WorldShapes.NORMAL, offered.get(0));
        assertTrue(offered.contains(cylinder), offered.toString());
    }

    @Test
    void creatingAWorldThroughTheRegistryLeavesTheShapeInTheGenerator() {
        WorldDimensions plain = vanillaOverworld();
        assertNull(ShapedDimensions.getShape(plain, LevelStem.OVERWORLD));

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(registries, plain);

        assertEquals(CYLINDER, ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
    }

    @Test
    void reCreateAdoptsTheShapeThatClaimsTheWorld() {
        WorldShapes.restoreFromExisting(registries, vanillaOverworld());
        assertNull(createdOverworldShape());

        WorldDimensions created = cylinder.atCreation().apply(registries, vanillaOverworld());
        WorldShapes.restoreFromExisting(registries, created);

        assertEquals(CYLINDER, createdOverworldShape());
    }

    @Test
    void reCreatingWithNormalSelectedDropsTheInheritedShape() {
        WorldDimensions inherited = cylinder.atCreation().apply(registries, vanillaOverworld());
        NoiseBasedChunkGenerator vanilla = overworldGenerator(vanillaOverworld());

        WorldShapes.select(WorldShapes.NORMAL);
        WorldDimensions created = WorldShapes.applyAtCreation(registries, inherited);

        ChunkGenerator generator = created.dimensions().get(LevelStem.OVERWORLD).generator();
        assertNull(ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
        assertEquals(NoiseBasedChunkGenerator.class, generator.getClass());
        assertEquals(vanilla.generatorSettings(), ((NoiseBasedChunkGenerator) generator).generatorSettings());
    }

    @Test
    void reCreatingIntoAShapeLeavesNoStemTheNewShapeDoesNotWrite() {
        WorldDimensions inherited = ShapedDimensions.withShape(
                ShapedDimensions.withShape(vanillaOverworldAndNether(), LevelStem.OVERWORLD,
                        new CarriedShape(null, CYLINDER, ShapeOptions.DEFAULT)),
                LevelStem.NETHER, new CarriedShape(null, CYLINDER, ShapeOptions.DEFAULT));

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(registries, inherited);

        assertEquals(CYLINDER, ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
        assertNull(ShapedDimensions.getShape(created, LevelStem.NETHER));
    }

    @Test
    void reCreatingFromOneShapeIntoAnotherTakesTheNewGeometry() {
        WorldDimensions inherited =
                ShapedDimensions.withShape(vanillaOverworld(), LevelStem.OVERWORLD,
                        new CarriedShape(null, WIDER_CYLINDER, ShapeOptions.DEFAULT));

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(registries, inherited);

        assertEquals(CYLINDER, ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
    }

    @Test
    void openingTheScreenResetsEveryRegisteredShape() {
        WorldShapes.select(cylinder);

        WorldShapes.resetToDefault();

        assertNull(createdOverworldShape());
        assertTrue(settingsWereReset);
    }

    @Test
    void onlyAShapeWithAScreenLightsUpTheCustomizeButton() {
        assertNotNull(ShapeCustomizers.of(cylinder.id()));
        assertNull(ShapeCustomizers.of(WorldShapes.NORMAL.id()));
    }

    private static @Nullable FlatShape createdOverworldShape() {
        return ShapedDimensions.getShape(WorldShapes.applyAtCreation(registries, vanillaOverworld()), LevelStem.OVERWORLD);
    }

    @Test
    void theShapeGeometryPassesTheGeneratorFieldTheWireAndTheFold() {
        JsonElement stored = CarriedShape.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE, CYLINDER).getOrThrow();
        assertEquals(CYLINDER, CarriedShape.SHAPE_CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow(),
                stored.toString());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(Level.OVERWORLD, CYLINDER));
        assertEquals(CYLINDER, WrappingSettingsPayload.STREAM_CODEC.decode(buffer).shape());
        assertEquals(0, buffer.readableBytes());

        assertTrue(WorldFolds.of(CYLINDER).isWrapped());
        assertTrue(WorldFolds.of(CYLINDER).getChunkDomain(Direction.Axis.X).isOver(64));
        assertTrue(!WorldFolds.of(CYLINDER).getChunkDomain(Direction.Axis.Z).isOver(1_000_000));
    }
}
