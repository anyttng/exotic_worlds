package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
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
    private static final int OVERWORLD_WIDTH_CHUNKS = 128;
    private static final int NETHER_SCALE = 8;
    private static final int END_WIDTH_CHUNKS = 256;
    private static final ShapeOptions GENERATION_OPTIONS =
            ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.OFF);

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void theUnshapedPresetComesBackUnshaped() {
        WorldDimensions preset = WorldPresets.createNormalWorldDimensions(worldgen);

        assertNull(ShapedDimensions.getShape(rereadThroughNbt(preset), LevelStem.OVERWORLD));
    }

    @Test
    void everyShapedStemComesBackWithTheSameShape() {
        WorldDimensions shaped = shapedPreset();
        assertNotNull(ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD), "the fixture carries no shape to lose");

        WorldDimensions reread = rereadThroughNbt(shaped);

        assertEquals(ShapedDimensions.getShape(shaped, LevelStem.OVERWORLD),
                ShapedDimensions.getShape(reread, LevelStem.OVERWORLD));
        assertEquals(ShapedDimensions.getShape(shaped, LevelStem.NETHER),
                ShapedDimensions.getShape(reread, LevelStem.NETHER));
        assertEquals(ShapedDimensions.getShape(shaped, LevelStem.END),
                ShapedDimensions.getShape(reread, LevelStem.END));
        assertEquals(GENERATION_OPTIONS, carriedShapeOf(reread, LevelStem.OVERWORLD).getOptions());
        assertEquals(GENERATION_OPTIONS, carriedShapeOf(reread, LevelStem.NETHER).getOptions());
        assertEquals(GENERATION_OPTIONS, carriedShapeOf(reread, LevelStem.END).getOptions());
    }

    private static CarriedShape carriedShapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.getCarriedShape(dimensions, key);
        assertNotNull(carried, key.identifier().toString());
        return carried;
    }

    private static WorldDimensions shapedPreset() {
        return LayoutDimensions.apply(WorldPresets.createNormalWorldDimensions(worldgen),
                LayoutFixtures.shaped(ShapeFixtures.torus(OVERWORLD_WIDTH_CHUNKS), NETHER_SCALE,
                        ShapeFixtures.torus(END_WIDTH_CHUNKS), GENERATION_OPTIONS));
    }

    private static WorldDimensions rereadThroughNbt(WorldDimensions dimensions) {
        RegistryOps<Tag> ops = worldgen.createSerializationContext(NbtOps.INSTANCE);
        Tag written = WorldDimensions.CODEC.encoder().encodeStart(ops, dimensions).getOrThrow();
        return WorldDimensions.CODEC.decoder().parse(ops, written).getOrThrow();
    }
}
