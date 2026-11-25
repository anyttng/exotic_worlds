package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.migration.FormerNamespace;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.layout.LayoutDimensions;
import com.exoticworlds.shape.layout.LayoutFixtures;
import com.exoticworlds.shape.layout.WorldLayout;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

@Timeout(60)
class LegacyDimensionsTest {
    private static final String CURRENT_PREFIX = ExoticWorlds.MODID + ":";
    private static final String TYPE_KEY = "type";
    private static final String NOISE_TYPE = "minecraft:noise";
    private static final String STAMP_KEY = CURRENT_PREFIX + "shape";

    private static final int OVERWORLD_WIDTH_CHUNKS = 128;
    private static final int NETHER_SCALE = 8;
    private static final int END_WIDTH_CHUNKS = 256;

    private static final List<ResourceKey<LevelStem>> STEMS = List.of(LevelStem.OVERWORLD, LevelStem.NETHER,
            LevelStem.END);

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aTorusSavedOnItsOwnGeneratorTypeLoadsStampedOnTheBaseGeneratorAndIsWrittenBackSo()
            throws CommandSyntaxException {
        assertMigrates(LayoutDimensions.apply(WorldPresets.createNormalWorldDimensions(worldgen), torus()),
                CURRENT_PREFIX);
    }

    @Test
    void aFlatTorusSavedOnItsOwnGeneratorTypeLoadsStampedOnTheBaseGeneratorAndIsWrittenBackSo()
            throws CommandSyntaxException {
        assertMigrates(flatTorus(), CURRENT_PREFIX);
    }

    @Test
    void aTorusSavedUnderTheFormerNamespaceLoadsAndIsWrittenBackUnderTheCurrentOne() throws CommandSyntaxException {
        assertMigrates(LayoutDimensions.apply(WorldPresets.createNormalWorldDimensions(worldgen), torus()),
                FormerNamespace.KEY_PREFIX);
    }

    @Test
    void aFlatTorusSavedUnderTheFormerNamespaceLoadsAndIsWrittenBackUnderTheCurrentOne()
            throws CommandSyntaxException {
        assertMigrates(flatTorus(), FormerNamespace.KEY_PREFIX);
    }

    @Test
    void aCylinderSavedUnderTheFormerNamespaceLoadsAndIsWrittenBackUnderTheCurrentOne()
            throws CommandSyntaxException {
        assertMigrates(LayoutDimensions.apply(WorldPresets.createNormalWorldDimensions(worldgen),
                LayoutFixtures.defaultCylinder()), FormerNamespace.KEY_PREFIX);
    }

    private static void assertMigrates(WorldDimensions saved, String savedPrefix) throws CommandSyntaxException {
        RegistryOps<Tag> ops = worldgen.createSerializationContext(NbtOps.INSTANCE);
        Tag current = withoutShapeIds(WorldDimensions.CODEC.encoder().encodeStart(ops, saved).getOrThrow());
        String former = onOwnGeneratorTypes(current.copy()).toString().replace(CURRENT_PREFIX, savedPrefix);
        assertTrue(former.contains(savedPrefix + LegacyGenerators.TOROIDAL_ID),
                "the fixture names no own generator type: " + former);

        WorldDimensions loaded = WorldDimensions.CODEC.decoder().parse(ops, TagParser.parseCompoundFully(former))
                .getOrThrow();
        for (ResourceKey<LevelStem> stem : STEMS) {
            assertEquals(carriedShapeOf(saved, stem).getShape(), carriedShapeOf(loaded, stem).getShape(), stem.toString());
            assertTrue(carriedShapeOf(loaded, stem).getShapeId().isEmpty(), stem.toString());
            assertEquals(carriedShapeOf(saved, stem).getOptions(),
                    carriedShapeOf(loaded, stem).getOptions(), stem.toString());
        }

        Tag rewritten = WorldDimensions.CODEC.encoder().encodeStart(ops, loaded).getOrThrow();
        assertFalse(rewritten.toString().contains(FormerNamespace.NAMESPACE), rewritten.toString());
        assertEquals(current, rewritten);
    }

    private static Tag onOwnGeneratorTypes(Tag tag) {
        if (tag instanceof CompoundTag compound && compound.contains(STAMP_KEY)) {
            CompoundTag stamp = compound.getCompound(STAMP_KEY).orElseThrow();
            compound.remove(STAMP_KEY);
            stamp.forEach(compound::put);
            compound.putString(TYPE_KEY, CURRENT_PREFIX + (NOISE_TYPE.equals(compound.getStringOr(TYPE_KEY, ""))
                    ? LegacyGenerators.TOROIDAL_ID
                    : LegacyGenerators.TOROIDAL_FLAT_ID));
        } else if (tag instanceof CompoundTag compound) {
            compound.keySet().forEach(key -> onOwnGeneratorTypes(compound.get(key)));
        } else if (tag instanceof ListTag list) {
            list.forEach(LegacyDimensionsTest::onOwnGeneratorTypes);
        }

        return tag;
    }

    private static WorldDimensions flatTorus() {
        WorldDimensions flat = worldgen.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value()
                .createWorldDimensions();
        return LayoutDimensions.apply(flat, torus());
    }

    private static Tag withoutShapeIds(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            compound.remove(DimensionTable.SHAPE_ID_KEY);
            compound.getCompound(STAMP_KEY).ifPresent(stamp -> stamp.remove(CarriedShape.ID_KEY));
            compound.keySet().forEach(key -> withoutShapeIds(compound.get(key)));
        } else if (tag instanceof ListTag list) {
            list.forEach(LegacyDimensionsTest::withoutShapeIds);
        }

        return tag;
    }

    private static WorldLayout torus() {
        return LayoutFixtures.shaped(ShapeFixtures.torus(OVERWORLD_WIDTH_CHUNKS), NETHER_SCALE,
                ShapeFixtures.torus(END_WIDTH_CHUNKS), LayoutFixtures.PLAIN);
    }

    private static CarriedShape carriedShapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.getCarriedShape(dimensions, key);
        assertNotNull(carried, key.identifier().toString());
        return carried;
    }
}
