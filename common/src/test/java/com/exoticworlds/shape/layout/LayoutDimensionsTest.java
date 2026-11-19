package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.custom.CustomDimensions;
import com.exoticworlds.shape.custom.CustomSettings;
import com.exoticworlds.shape.custom.DimensionShape;
import com.exoticworlds.shape.cylinder.CylinderDimensions;
import com.exoticworlds.shape.cylinder.CylinderSettings;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.TorusDimensions;
import com.exoticworlds.shape.torus.TorusSettings;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class LayoutDimensionsTest {
    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createLookup();

    private static final GenerationOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    private static final GenerationOptions COMPACT_6 = PLAIN.with(CompactBiomes.OPTION, ClimateScale.custom(6));

    private static final GenerationOptions LAND = PLAIN.with(GuaranteedLand.OPTION, true);

    private static final ResourceKey<LevelStem> MOD_STEM = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("example_mod", "sky"));

    @Test
    void theDefaultLayoutLeavesTheWorldAsItWas() {
        WorldDimensions created = LayoutDimensions.apply(vanillaDimensions(), WorldLayout.DEFAULT);

        assertNull(ShapeDimensions.spansOf(created, LevelStem.OVERWORLD));
        assertNull(ShapeDimensions.spansOf(created, LevelStem.NETHER));
        assertNull(ShapeDimensions.spansOf(created, LevelStem.END));
        assertNull(LayoutDimensions.read(created));
    }

    @Test
    void eachDimensionKeepsItsOwnShapeAndOptions() {
        WorldLayout chosen = new WorldLayout(
                DimensionLayout.normal(PLAIN),
                DimensionLayout.looped(LoopSpans.ofWidths(24, 40), COMPACT_6),
                DimensionLayout.looped(LoopSpans.ofWidth(Direction.Axis.X, 256), PLAIN),
                Map.of(), NetherScales.DEFAULT);

        assertEquals(chosen, roundTrip(chosen));
    }

    @Test
    void aSkewedTorusWithTheNetherAndTheEndFromItRoundTrips() {
        WorldLayout chosen = new WorldLayout(
                DimensionLayout.looped(LoopSpans.ofWidths(64, 96).withSkew(8), LAND),
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
                DimensionLayout.looped(LoopSpans.ofWidth(Direction.Axis.Z, 128), PLAIN),
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
                DimensionLayout.looped(LoopSpans.ofWidth(32), PLAIN),
                DimensionLayout.fromOverworld(PLAIN),
                DimensionLayout.fromOverworld(PLAIN),
                Map.of(), 8);

        assertEquals(LoopSpans.ofWidth(16), chosen.netherSpans());
        assertEquals(2, roundTrip(chosen).netherScale());
    }

    @Test
    void aWorldTheToroidalShapeMadeReadsWithItsSizesScaleAndEnd() {
        TorusSettings torus = new TorusSettings(LoopSpans.ofWidths(64, 128).withSkew(16), 4, LoopSpans.ofWidth(320), LAND);

        WorldLayout read = LayoutDimensions.read(TorusDimensions.apply(vanillaDimensions(), torus));

        assertNotNull(read);
        assertEquals(DimensionLayout.looped(LoopSpans.ofWidths(64, 128).withSkew(16), LAND), read.overworld());
        assertEquals(ShapeKind.FROM_OVERWORLD, read.nether().kind());
        assertEquals(4, read.netherScale());
        assertEquals(LoopSpans.ofWidth(320), read.end().spans());
        assertEquals(TorusDimensions.apply(vanillaDimensions(), torus).dimensions().keySet(),
                LayoutDimensions.apply(vanillaDimensions(), read).dimensions().keySet());
        assertSameShapes(TorusDimensions.apply(vanillaDimensions(), torus),
                LayoutDimensions.apply(vanillaDimensions(), read));
    }

    @Test
    void aWorldTheCylinderShapeMadeReadsWithItsAxisSizeAndScale() {
        CylinderSettings cylinder = new CylinderSettings(LoopSpans.ofWidth(Direction.Axis.X, 128), 8,
                LoopSpans.ofWidth(Direction.Axis.X, 256), PLAIN.with(SpawnPoint.OPTION, new SpawnPoint(4, -3)));
        WorldDimensions made = CylinderDimensions.apply(vanillaDimensions(), cylinder);

        WorldLayout read = LayoutDimensions.read(made);

        assertNotNull(read);
        assertEquals(ShapeKind.CYLINDER, read.overworld().kind());
        assertEquals(8, read.netherScale());
        assertSameShapes(made, LayoutDimensions.apply(vanillaDimensions(), read));
    }

    @Test
    void aWorldTheCustomShapeMadeReadsDimensionByDimension() {
        CustomSettings custom = new CustomSettings(
                new DimensionShape(null, CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidths(24, 40), COMPACT_6),
                DimensionShape.fromOverworld(CustomSettings.DEFAULT_OPTIONS));
        WorldDimensions made = CustomDimensions.apply(vanillaDimensions(), custom);

        WorldLayout read = LayoutDimensions.read(made);

        assertNotNull(read);
        assertEquals(ShapeKind.NORMAL, read.overworld().kind());
        assertEquals(DimensionLayout.looped(LoopSpans.ofWidths(24, 40), COMPACT_6), read.nether());
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
    void theSpawnPointMovesOnlyOnALoopedOverworld() {
        SpawnPoint spawn = new SpawnPoint(5, -7);
        WorldLayout looped = new WorldLayout(DimensionLayout.looped(LoopSpans.ofWidth(64), PLAIN),
                DimensionLayout.fromOverworld(PLAIN), DimensionLayout.fromOverworld(PLAIN), Map.of(),
                NetherScales.DEFAULT);

        WorldLayout moved = looped.withSpawn(spawn);

        assertNotNull(moved);
        assertEquals(spawn, moved.overworld().options().get(SpawnPoint.OPTION));
        assertNull(WorldLayout.DEFAULT.withSpawn(spawn));
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
                new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(noise));
        return new LevelStem(WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(type), generator);
    }
}
