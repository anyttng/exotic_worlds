package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.engine.gen.ShapedDimensions;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;

import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
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

class LayoutDimensionsTest {
    private static HolderLookup.Provider worldgen;

    private static final ShapeOptions PLAIN = LayoutFixtures.PLAIN;

    private static final ShapeOptions COMPACT_6 = PLAIN.with(CompactBiomes.FEATURE.getOption(), ClimateScale.custom(6));

    private static final ShapeOptions LAND = PLAIN.with(GuaranteedLand.FEATURE.getOption(), true);

    private static final ResourceKey<LevelStem> MOD_STEM = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("example_mod", "sky"));

    private static final ResourceKey<LevelStem> OTHER_MOD_STEM = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("example_mod", "mine"));

    @BeforeAll
    static void registerShapes() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void theDefaultLayoutLeavesTheWorldAsItWas() {
        WorldDimensions created = LayoutDimensions.apply(vanillaDimensions(), WorldLayout.newWorld());

        assertNull(ShapeDimensions.spansOf(created, LevelStem.OVERWORLD));
        assertNull(ShapeDimensions.spansOf(created, LevelStem.NETHER));
        assertNull(ShapeDimensions.spansOf(created, LevelStem.END));
        assertNull(LayoutDimensions.read(created));
    }

    @Test
    void eachDimensionKeepsItsOwnShapeAndOptions() {
        WorldLayout chosen = new WorldLayout(
                DimensionLayout.normal(PLAIN),
                LayoutFixtures.looped(ShapeFixtures.torus(24, 40), COMPACT_6),
                LayoutFixtures.looped(ShapeFixtures.cylinder(Direction.Axis.X, 256), PLAIN),
                Map.of(), NetherScales.DEFAULT);

        assertEquals(chosen, roundTrip(chosen));
    }

    @Test
    void aSkewedTorusWithTheNetherAndTheEndFromItRoundTrips() {
        WorldLayout chosen = new WorldLayout(
                LayoutFixtures.looped(ShapeFixtures.latticeTorus(64, 96, 8), LAND),
                DimensionLayout.fromOverworld(COMPACT_6),
                DimensionLayout.fromOverworld(PLAIN),
                Map.of(), 4);

        WorldDimensions created = LayoutDimensions.apply(vanillaDimensions(), chosen);

        assertEquals(LoopSpans.ofWidths(16, 24).withSkew(2), ShapeDimensions.spansOf(created, LevelStem.NETHER));
        assertEquals(LoopSpans.ofWidths(240, 272), ShapeDimensions.spansOf(created, LevelStem.END));
        assertEquals(chosen, LayoutDimensions.read(created));
    }

    @Test
    void aCylinderGivesTheNetherAndTheEndItsLoopingAxis() {
        WorldLayout chosen = new WorldLayout(
                LayoutFixtures.looped(ShapeFixtures.cylinder(Direction.Axis.Z, 128), PLAIN),
                DimensionLayout.fromOverworld(PLAIN),
                DimensionLayout.fromOverworld(PLAIN),
                Map.of(), 8);

        WorldDimensions created = LayoutDimensions.apply(vanillaDimensions(), chosen);

        assertEquals(LoopSpans.ofWidth(Direction.Axis.Z, 16), ShapeDimensions.spansOf(created, LevelStem.NETHER));
        assertEquals(LoopSpans.ofWidth(Direction.Axis.Z, 304), ShapeDimensions.spansOf(created, LevelStem.END));
        assertEquals(chosen, LayoutDimensions.read(created));
    }

    @Test
    void aNetherScaleTheOverworldCannotTakeFallsToOneItCan() {
        WorldLayout chosen = new WorldLayout(
                LayoutFixtures.looped(ShapeFixtures.torus(32), PLAIN),
                DimensionLayout.fromOverworld(PLAIN),
                DimensionLayout.fromOverworld(PLAIN),
                Map.of(), 8);

        assertEquals(ShapeFixtures.torus(16), chosen.getNetherSpans());
        assertEquals(2, roundTrip(chosen).netherScale());
    }

    @Test
    void aToroidalWorldWrittenThroughThePublicWriterReadsWithItsSizesScaleAndEnd() {
        LoopSpans overworld = LoopSpans.ofWidths(64, 128).withSkew(16);
        WorldDimensions made = ShapeDimensions.withSpans(vanillaDimensions(), overworld, overworld.scaledDown(4),
                LoopSpans.ofWidth(320), OptionBridge.toV1(LAND));

        WorldLayout read = LayoutDimensions.read(made);

        assertNotNull(read);
        assertEquals(LayoutFixtures.looped(ShapeFixtures.latticeTorus(64, 128, 16), LAND), read.overworld());
        assertEquals(ShapeKind.FROM_OVERWORLD, read.nether().kind());
        assertEquals(4, read.netherScale());
        assertEquals(ShapeFixtures.torus(320), read.end().spans());
        assertSameShapes(made, LayoutDimensions.apply(vanillaDimensions(), read));
    }

    @Test
    void aCylinderWorldWrittenThroughThePublicWriterReadsWithItsAxisSizeAndScale() {
        LoopSpans overworld = LoopSpans.ofWidth(Direction.Axis.X, 128);
        WorldDimensions made = ShapeDimensions.withSpans(vanillaDimensions(), overworld, overworld.scaledDown(8),
                LoopSpans.ofWidth(Direction.Axis.X, 256),
                OptionBridge.toV1(PLAIN.with(SpawnPoint.OPTION, new SpawnPoint(4, -3))));

        WorldLayout read = LayoutDimensions.read(made);

        assertNotNull(read);
        assertEquals(DeclaredShapes.CYLINDER, read.overworld().shape());
        assertEquals(8, read.netherScale());
        assertSameShapes(made, LayoutDimensions.apply(vanillaDimensions(), read));
    }

    @Test
    void theOverworldAndAModRowRefuseFromOverworld() {
        DimensionLayout fromOverworld = DimensionLayout.fromOverworld(PLAIN);
        DimensionLayout normal = DimensionLayout.normal(PLAIN);

        assertThrows(IllegalArgumentException.class,
                () -> new WorldLayout(fromOverworld, normal, normal, Map.of(), NetherScales.DEFAULT));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldLayout(normal, normal, normal, Map.of(MOD_STEM, fromOverworld), NetherScales.DEFAULT));
    }

    @Test
    void aModDimensionWithNoRowIsFromTheOverworldAndChoosingThatDropsItsRow() {
        DimensionLayout own = LayoutFixtures.looped(ShapeFixtures.torus(48), COMPACT_6);

        WorldLayout withRow = WorldLayout.newWorld().withModDimension(MOD_STEM, own);

        assertEquals(ShapeKind.FROM_OVERWORLD, WorldLayout.newWorld().getModDimension(MOD_STEM).kind());
        assertEquals(own, withRow.getModDimension(MOD_STEM));
        assertEquals(Map.of(), withRow.withModDimension(MOD_STEM, DimensionLayout.fromOverworld(PLAIN)).modDimensions());
    }

    @Test
    void aModDimensionFromTheOverworldTakesItsSizeOverTheCoordinateScales() {
        FlatShape overworld = ShapeFixtures.latticeTorus(128, 64, 16);

        assertEquals(overworld, OverworldDerivation.modDimension(overworld, 1.0, 1.0));
        assertEquals(ShapeFixtures.latticeTorus(32, 16, 4), OverworldDerivation.modDimension(overworld, 1.0, 4.0));
        assertNull(OverworldDerivation.modDimension(ShapeFixtures.torus(100), 1.0, 8.0));
        assertNull(OverworldDerivation.modDimension(null, 1.0, 1.0));
    }

    @Test
    void theRowsAServerSendsReadAsTheWorldsLayout() {
        DimensionLayout overworld = LayoutFixtures.looped(ShapeFixtures.torus(64), LAND);
        DimensionLayout sky = LayoutFixtures.looped(ShapeFixtures.cylinder(Direction.Axis.Z, 48), COMPACT_6);
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> rows = new HashMap<>();
        rows.put(LevelStem.OVERWORLD, LayoutDimensions.entryOf(overworld));
        rows.put(LevelStem.NETHER, LayoutDimensions.entryOf(LayoutFixtures.looped(ShapeFixtures.torus(16), PLAIN)));
        rows.put(LevelStem.END, DimensionTable.Entry.UNWRAPPED);
        rows.put(MOD_STEM, LayoutDimensions.entryOf(sky));
        rows.put(OTHER_MOD_STEM, LayoutDimensions.entryOf(DimensionLayout.fromOverworld(PLAIN)));

        WorldLayout read = LayoutDimensions.ofRows(rows);

        assertEquals(overworld, read.overworld());
        assertEquals(ShapeKind.FROM_OVERWORLD, read.nether().kind());
        assertEquals(4, read.netherScale());
        assertEquals(ShapeKind.NORMAL, read.end().kind());
        assertEquals(Map.of(MOD_STEM, sky), read.modDimensions());
    }

    @Test
    void rowsStoredWithoutAShapeIdReadAsTheTorusOrTheCylinderTheirSpansMake() {
        FlatShape torus = ShapeFixtures.torus(64);
        FlatShape cylinder = ShapeFixtures.cylinder(Direction.Axis.Z, 48);
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> rows = new HashMap<>();
        rows.put(LevelStem.OVERWORLD, new DimensionTable.Entry(new CarriedShape(null, torus, PLAIN)));
        rows.put(MOD_STEM, new DimensionTable.Entry(new CarriedShape(null, cylinder, PLAIN)));

        WorldLayout read = LayoutDimensions.ofRows(rows);

        assertEquals(DimensionLayout.shaped(DeclaredShapes.TORUS, torus, PLAIN), read.overworld());
        assertEquals(DimensionLayout.shaped(DeclaredShapes.CYLINDER, cylinder, PLAIN), read.getModDimension(MOD_STEM));
    }

    @Test
    void theSpawnPointMovesOnlyOnALoopedOverworld() {
        SpawnPoint spawn = new SpawnPoint(5, -7);
        WorldLayout looped = new WorldLayout(LayoutFixtures.looped(ShapeFixtures.torus(64), PLAIN),
                DimensionLayout.fromOverworld(PLAIN), DimensionLayout.fromOverworld(PLAIN), Map.of(),
                NetherScales.DEFAULT);

        WorldLayout moved = looped.withSpawn(spawn);

        assertNotNull(moved);
        assertEquals(spawn, moved.overworld().options().get(SpawnPoint.OPTION));
        assertNull(WorldLayout.newWorld().withSpawn(spawn));
    }

    @Test
    void eachWrappedDimensionIsStoredWithTheIdOfItsShapeAndADerivedOneWithTheOverworlds() {
        WorldLayout chosen = new WorldLayout(LayoutFixtures.looped(ShapeFixtures.torus(48), PLAIN),
                DimensionLayout.fromOverworld(PLAIN),
                LayoutFixtures.looped(ShapeFixtures.cylinder(Direction.Axis.X, 256), PLAIN),
                Map.of(), NetherScales.DEFAULT);
        WorldDimensions applied = LayoutDimensions.apply(vanillaDimensions(), chosen);

        assertEquals(Optional.of(DeclaredShapes.TORUS), shapeIdOf(applied, LevelStem.OVERWORLD));
        assertEquals(Optional.of(DeclaredShapes.TORUS), shapeIdOf(applied, LevelStem.NETHER));
        assertEquals(Optional.of(DeclaredShapes.CYLINDER), shapeIdOf(applied, LevelStem.END));
    }

    private static Optional<Identifier> shapeIdOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.getCarriedShape(dimensions, key);
        assertNotNull(carried);
        return carried.getShapeId();
    }

    private static WorldLayout roundTrip(WorldLayout chosen) {
        WorldLayout read = LayoutDimensions.read(LayoutDimensions.apply(vanillaDimensions(), chosen));
        assertNotNull(read);
        return read;
    }

    private static void assertSameShapes(WorldDimensions expected, WorldDimensions actual) {
        for (ResourceKey<LevelStem> key : expected.dimensions().keySet()) {
            assertEquals(ShapeDimensions.spansOf(expected, key), ShapeDimensions.spansOf(actual, key), key.toString());
            assertEquals(ShapeDimensions.optionsOf(expected, key), ShapeDimensions.optionsOf(actual, key),
                    key.toString());
        }
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
