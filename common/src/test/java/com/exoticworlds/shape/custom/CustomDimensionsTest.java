package com.exoticworlds.shape.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.cylinder.CylinderDimensions;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.TorusDimensions;
import com.exoticworlds.shape.torus.TorusSettings;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class CustomDimensionsTest {
    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createWorldLookup();

    private static final GenerationOptions COMPACT_6 = CustomSettings.DEFAULT_OPTIONS.with(CompactBiomes.OPTION,
            ClimateScale.custom(6));

    private static final GenerationOptions LAND = CustomSettings.DEFAULT_OPTIONS.with(GuaranteedLand.OPTION, true);

    private static final DimensionShape NONE = new DimensionShape(null, CustomSettings.DEFAULT_OPTIONS);

    private static final CustomSettings PLAIN_OVERWORLD = new CustomSettings(
            NONE,
            new DimensionShape(LoopSpans.ofWidths(24, 40), COMPACT_6),
            new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 256), CustomSettings.DEFAULT_OPTIONS));

    @Test
    void aPlainOverworldBesideAWrappedNetherAndEndRoundTrips() {
        WorldDimensions created = CustomDimensions.apply(vanillaDimensions(), PLAIN_OVERWORLD);

        assertNull(ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertEquals(ShapeFixtures.torus(WorldLoopBounds.ofWidths(24, 40)),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(ShapeFixtures.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 256)),
                ShapedDimensions.shapeOf(created, LevelStem.END));
        assertEquals(PLAIN_OVERWORLD, CustomDimensions.read(created));
    }

    @Test
    void eachDimensionKeepsItsOwnOptions() {
        CustomSettings chosen = new CustomSettings(
                new DimensionShape(LoopSpans.ofWidth(32), LAND),
                new DimensionShape(LoopSpans.ofWidth(Direction.Axis.Z, 24), COMPACT_6),
                NONE);
        CustomSettings read = CustomDimensions.read(CustomDimensions.apply(vanillaDimensions(), chosen));

        assertEquals(LAND, read.overworld().options());
        assertEquals(COMPACT_6, read.nether().options());
    }

    @Test
    void torusAndCylinderLeaveAMixedWorldToCustom() {
        WorldDimensions torusOverCylinder = CustomDimensions.apply(vanillaDimensions(), new CustomSettings(
                new DimensionShape(LoopSpans.ofWidth(64), CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 16), CustomSettings.DEFAULT_OPTIONS),
                NONE));
        WorldDimensions cylinderOverTorus = CustomDimensions.apply(vanillaDimensions(), new CustomSettings(
                new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 64), CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidth(16), CustomSettings.DEFAULT_OPTIONS),
                new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 256), CustomSettings.DEFAULT_OPTIONS)));

        assertNull(TorusDimensions.read(torusOverCylinder));
        assertNull(CylinderDimensions.read(cylinderOverTorus));
        assertEquals(StemKind.CYLINDER, CustomDimensions.read(torusOverCylinder).nether().kind());
        assertEquals(LoopSpans.ofWidth(16), CustomDimensions.read(cylinderOverTorus).nether().spans());
    }

    @Test
    void aWorldNoDimensionOfWhichWrapsIsNotCustom() {
        assertNull(CustomDimensions.read(vanillaDimensions()));
    }

    @Test
    void aSkewedTorusIsNotCustom() {
        WorldDimensions skewed = TorusDimensions.apply(vanillaDimensions(), new TorusSettings(
                LoopSpans.ofWidth(64).withSkew(8), 4, LoopSpans.ofWidth(320), TorusSettings.DEFAULT.generationOptions()));

        assertNull(CustomDimensions.read(skewed));
    }

    @Test
    void aDimensionShapeRefusesASkew() {
        assertThrows(IllegalArgumentException.class,
                () -> new DimensionShape(LoopSpans.ofWidth(64).withSkew(8), CustomSettings.DEFAULT_OPTIONS));
    }

    @Test
    void changingTheKindKeepsTheLoopedWidth() {
        DimensionShape cylinder = new DimensionShape(LoopSpans.ofWidth(Direction.Axis.Z, 48), COMPACT_6);

        assertEquals(new DimensionShape(LoopSpans.ofWidth(48), COMPACT_6),
                cylinder.withKind(StemKind.TORUS, VanillaDimension.NETHER));
        assertEquals(new DimensionShape(LoopSpans.ofWidth(Direction.Axis.X, 256), COMPACT_6),
                new DimensionShape(null, COMPACT_6).withKind(StemKind.CYLINDER, VanillaDimension.END));
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
