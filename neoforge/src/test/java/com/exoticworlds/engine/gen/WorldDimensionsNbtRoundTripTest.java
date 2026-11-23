package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.layout.LayoutDimensions;
import com.exoticworlds.shape.layout.LayoutFixtures;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

@Timeout(60)
class WorldDimensionsNbtRoundTripTest {
    private static final int OVERWORLD_CHUNK_WIDTH = 128;
    private static final int NETHER_SCALE = 8;
    private static final int END_CHUNK_WIDTH = 256;
    private static final ShapeOptions GENERATION_OPTIONS =
            ShapeOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF);

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createWorldLookup();
    }

    @Test
    void theUnshapedPresetComesBackUnshaped() {
        WorldDimensions preset = WorldPresets.createNormalWorldDimensions(worldgen);

        assertNull(ShapedDimensions.shapeOf(rereadThroughNbt(preset), LevelStem.OVERWORLD));
    }

    @Test
    void everyShapedStemComesBackWithTheSameShape() {
        WorldDimensions shaped = shapedPreset();
        assertNotNull(ShapedDimensions.shapeOf(shaped, LevelStem.OVERWORLD), "the fixture carries no shape to lose");

        WorldDimensions reread = rereadThroughNbt(shaped);

        assertEquals(ShapedDimensions.shapeOf(shaped, LevelStem.OVERWORLD),
                ShapedDimensions.shapeOf(reread, LevelStem.OVERWORLD));
        assertEquals(ShapedDimensions.shapeOf(shaped, LevelStem.NETHER),
                ShapedDimensions.shapeOf(reread, LevelStem.NETHER));
        assertEquals(ShapedDimensions.shapeOf(shaped, LevelStem.END),
                ShapedDimensions.shapeOf(reread, LevelStem.END));
        assertEquals(GENERATION_OPTIONS, carriedShapeOf(reread, LevelStem.OVERWORLD).options());
        assertEquals(GENERATION_OPTIONS, carriedShapeOf(reread, LevelStem.NETHER).options());
        assertEquals(GENERATION_OPTIONS, carriedShapeOf(reread, LevelStem.END).options());
    }

    private static CarriedShape carriedShapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.carriedShapeOf(dimensions, key);
        assertNotNull(carried, key.identifier().toString());
        return carried;
    }

    private static WorldDimensions shapedPreset() {
        return LayoutDimensions.apply(WorldPresets.createNormalWorldDimensions(worldgen),
                LayoutFixtures.shaped(ShapeFixtures.torus(OVERWORLD_CHUNK_WIDTH), NETHER_SCALE,
                        ShapeFixtures.torus(END_CHUNK_WIDTH), GENERATION_OPTIONS));
    }

    private static WorldDimensions rereadThroughNbt(WorldDimensions dimensions) {
        RegistryOps<Tag> ops = worldgen.createSerializationContext(NbtOps.INSTANCE);
        Tag written = WorldDimensions.CODEC.encoder().encodeStart(ops, dimensions).getOrThrow();
        return WorldDimensions.CODEC.decoder().parse(ops, written).getOrThrow();
    }
}
