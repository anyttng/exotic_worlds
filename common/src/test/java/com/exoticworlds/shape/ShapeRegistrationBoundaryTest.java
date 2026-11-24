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
import com.exoticworlds.api.v1.shape.ShapeModule;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.gen.ShapedChunkGenerator;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;
import com.exoticworlds.engine.net.WrappingSettingsPayload;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createWorldLookup();

    private static final Identifier CYLINDER_ID =
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "boundary_test_cylinder");

    private static final Identifier MODULE_ID =
            Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "boundary_test_module");

    private static final Identifier FOREIGN_ID = Identifier.fromNamespaceAndPath("boundary_test_mod", "band");

    private static final FlatShape CYLINDER = ShapeFixtures.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE));

    private static final FlatShape WIDER_CYLINDER = ShapeFixtures.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-64, 64), AxisBounds.Unbounded.INSTANCE));

    private static WorldShape cylinder;

    private static final ShapeModule<FlatShape> MODULE = ShapeModule.of(
            MODULE_ID,
            CYLINDER,
            (dimensions, shape) ->
                    ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD,
                            new CarriedShape(shape, ShapeOptions.DEFAULT)),
            dimensions -> {
                FlatShape shape = ShapedDimensions.getShape(dimensions, LevelStem.OVERWORLD);
                return WIDER_CYLINDER.equals(shape) ? shape : null;
            });

    private static boolean settingsWereReset;

    @BeforeAll
    static void registerTheShapeThroughThePublicSeamsAlone() {
        cylinder = WorldShape.of(
                CYLINDER_ID,
                Component.literal("Boundary test cylinder"),
                Component.literal("A cylinder registered by the boundary test"),
                (registries, dimensions) -> ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD,
                        new CarriedShape(CYLINDER, ShapeOptions.DEFAULT)),
                () -> settingsWereReset = true,
                (registries, dimensions) ->
                        CYLINDER.equals(ShapedDimensions.getShape(dimensions, LevelStem.OVERWORLD)));

        WorldShapes.register(WorldShape.of(
                FOREIGN_ID,
                Component.literal("Boundary test band"),
                Component.literal("A shape registered from another namespace"),
                (registries, dimensions) -> dimensions));

        WorldShapes.register(cylinder);
        ShapeCustomizers.register(CYLINDER_ID, parent -> parent);
        MODULE.register();
    }

    @AfterEach
    void leaveTheRegistryAsItWasFound() {
        WorldShapes.select(WorldShapes.NORMAL);
        settingsWereReset = false;
    }

    private static WorldDimensions vanillaOverworld() {
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD));

        return new WorldDimensions(Map.of(LevelStem.OVERWORLD, new LevelStem(
                WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                generator)));
    }

    private static WorldDimensions vanillaOverworldAndNether() {
        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>(vanillaOverworld().dimensions());
        stems.put(LevelStem.NETHER, new LevelStem(
                WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.NETHER),
                new NoiseBasedChunkGenerator(
                        new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.NETHER_WASTES)),
                        WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.NETHER))));
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
    void theModsOwnShapesKeepTheirRegistrationOrderAndForeignOnesFollow() {
        List<Identifier> offered = WorldShapes.getShapes().stream().map(WorldShape::id).toList();

        assertTrue(offered.indexOf(CYLINDER_ID) < offered.indexOf(MODULE_ID), offered.toString());
        assertTrue(offered.indexOf(MODULE_ID) < offered.indexOf(FOREIGN_ID), offered.toString());
    }

    @Test
    void creatingAWorldThroughTheRegistryLeavesTheShapeInTheGenerator() {
        WorldDimensions plain = vanillaOverworld();
        assertNull(ShapedDimensions.getShape(plain, LevelStem.OVERWORLD));

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, plain);

        assertEquals(CYLINDER, ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
    }

    @Test
    void reCreateAdoptsTheShapeThatClaimsTheWorld() {
        WorldShapes.restoreFromExisting(REGISTRIES, vanillaOverworld());
        assertNull(createdOverworldShape());

        WorldDimensions created = cylinder.atCreation().apply(REGISTRIES, vanillaOverworld());
        WorldShapes.restoreFromExisting(REGISTRIES, created);

        assertEquals(CYLINDER, createdOverworldShape());
    }

    @Test
    void reCreatingWithNormalSelectedDropsTheInheritedShape() {
        WorldDimensions inherited = cylinder.atCreation().apply(REGISTRIES, vanillaOverworld());
        NoiseBasedChunkGenerator vanilla = overworldGenerator(vanillaOverworld());

        WorldShapes.select(WorldShapes.NORMAL);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, inherited);

        ChunkGenerator generator = created.dimensions().get(LevelStem.OVERWORLD).generator();
        assertNull(ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
        assertFalse(generator instanceof ShapedChunkGenerator, generator.getClass().getName());
        assertEquals(vanilla.generatorSettings(), ((NoiseBasedChunkGenerator) generator).generatorSettings());
    }

    @Test
    void reCreatingIntoAShapeLeavesNoStemTheNewShapeDoesNotWrite() {
        WorldDimensions inherited = ShapedDimensions.withShape(
                ShapedDimensions.withShape(vanillaOverworldAndNether(), LevelStem.OVERWORLD,
                        new CarriedShape(CYLINDER, ShapeOptions.DEFAULT)),
                LevelStem.NETHER, new CarriedShape(CYLINDER, ShapeOptions.DEFAULT));

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, inherited);

        assertEquals(CYLINDER, ShapedDimensions.getShape(created, LevelStem.OVERWORLD));
        assertNull(ShapedDimensions.getShape(created, LevelStem.NETHER));
    }

    @Test
    void reCreatingFromOneShapeIntoAnotherTakesTheNewGeometry() {
        WorldDimensions inherited =
                ShapedDimensions.withShape(vanillaOverworld(), LevelStem.OVERWORLD,
                        new CarriedShape(WIDER_CYLINDER, ShapeOptions.DEFAULT));

        WorldShapes.select(cylinder);
        WorldDimensions created = WorldShapes.applyAtCreation(REGISTRIES, inherited);

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

    @Test
    void aModuleAdoptsTheWorldItRecognisesAndResetsBackToItsOwnDefault() {
        WorldShape registered = registeredModuleShape();
        WorldDimensions wider = ShapedDimensions.withShape(
                vanillaOverworld(), LevelStem.OVERWORLD, new CarriedShape(WIDER_CYLINDER, ShapeOptions.DEFAULT));

        WorldShapes.restoreFromExisting(REGISTRIES, wider);

        assertEquals(WIDER_CYLINDER, createdOverworldShape());
        assertEquals(WIDER_CYLINDER, MODULE.settings());

        WorldShapes.resetToDefault();

        assertEquals(CYLINDER, MODULE.settings());
        assertEquals(CYLINDER, ShapedDimensions.getShape(
                registered.atCreation().apply(REGISTRIES, vanillaOverworld()), LevelStem.OVERWORLD));
    }

    private static @Nullable FlatShape createdOverworldShape() {
        return ShapedDimensions.getShape(WorldShapes.applyAtCreation(REGISTRIES, vanillaOverworld()), LevelStem.OVERWORLD);
    }

    private static WorldShape registeredModuleShape() {
        return WorldShapes.getShapes().stream()
                .filter(shape -> shape.id().equals(MODULE_ID))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theShapeGeometryPassesTheGeneratorFieldTheWireAndTheFold() {
        JsonElement stored = CarriedShape.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE, CYLINDER).getOrThrow();
        assertEquals(CYLINDER, CarriedShape.SHAPE_CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow(),
                stored.toString());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(Level.OVERWORLD, CYLINDER));
        assertEquals(CYLINDER, WrappingSettingsPayload.STREAM_CODEC.decode(buffer).shape());
        assertEquals(0, buffer.readableBytes());

        assertTrue(WorldFolds.of(CYLINDER).isWrapped());
        assertTrue(WorldFolds.of(CYLINDER).getChunkDomain(Direction.Axis.X).isOver(64));
        assertTrue(!WorldFolds.of(CYLINDER).getChunkDomain(Direction.Axis.Z).isOver(1_000_000));
    }
}
