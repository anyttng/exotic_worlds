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
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.migration.FormerNamespace;
import com.exoticworlds.shape.WorldOptionSetup;
import com.exoticworlds.shape.cylinder.CylinderDimensions;
import com.exoticworlds.shape.cylinder.CylinderSettings;
import com.exoticworlds.shape.torus.TorusDimensions;
import com.exoticworlds.shape.torus.TorusSettings;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
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
class FormerNamespaceDimensionsTest {
    private static final String CURRENT_PREFIX = ExoticWorlds.MODID + ":";
    private static final String TOROIDAL_TYPE = FormerNamespace.KEY_PREFIX + WorldLoopGenerators.TOROIDAL_ID;
    private static final String TOROIDAL_FLAT_TYPE = FormerNamespace.KEY_PREFIX + WorldLoopGenerators.TOROIDAL_FLAT_ID;

    private static final int OVERWORLD_CHUNK_WIDTH = 128;
    private static final int NETHER_SCALE = 8;
    private static final int END_CHUNK_WIDTH = 256;

    private static final List<ResourceKey<LevelStem>> STEMS = List.of(LevelStem.OVERWORLD, LevelStem.NETHER,
            LevelStem.END);

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aTorusSavedUnderTheFormerNamespaceLoadsAndIsWrittenBackUnderTheCurrentOne() throws CommandSyntaxException {
        assertMigrates(TorusDimensions.apply(WorldPresets.createNormalWorldDimensions(worldgen), torus()),
                TOROIDAL_TYPE);
    }

    @Test
    void aFlatTorusSavedUnderTheFormerNamespaceLoadsAndIsWrittenBackUnderTheCurrentOne()
            throws CommandSyntaxException {
        WorldDimensions flat = worldgen.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value()
                .createWorldDimensions();
        assertMigrates(TorusDimensions.apply(flat, torus()), TOROIDAL_FLAT_TYPE);
    }

    @Test
    void aCylinderSavedUnderTheFormerNamespaceLoadsAndIsWrittenBackUnderTheCurrentOne()
            throws CommandSyntaxException {
        assertMigrates(CylinderDimensions.apply(WorldPresets.createNormalWorldDimensions(worldgen),
                CylinderSettings.DEFAULT), TOROIDAL_TYPE);
    }

    private static void assertMigrates(WorldDimensions saved, String formerType) throws CommandSyntaxException {
        RegistryOps<Tag> ops = worldgen.createSerializationContext(NbtOps.INSTANCE);
        Tag current = WorldDimensions.CODEC.encoder().encodeStart(ops, saved).getOrThrow();
        String former = current.toString().replace(CURRENT_PREFIX, FormerNamespace.KEY_PREFIX);
        assertTrue(former.contains(formerType), "the fixture names no former generator type: " + former);

        WorldDimensions loaded = WorldDimensions.CODEC.decoder().parse(ops, TagParser.parseCompoundFully(former))
                .getOrThrow();
        for (ResourceKey<LevelStem> stem : STEMS) {
            assertEquals(carriedShapeOf(saved, stem).shape(), carriedShapeOf(loaded, stem).shape(), stem.toString());
            assertEquals(carriedShapeOf(saved, stem).generationOptions(),
                    carriedShapeOf(loaded, stem).generationOptions(), stem.toString());
        }

        Tag rewritten = WorldDimensions.CODEC.encoder().encodeStart(ops, loaded).getOrThrow();
        assertFalse(rewritten.toString().contains(FormerNamespace.NAMESPACE), rewritten.toString());
        assertEquals(current, rewritten);
    }

    private static TorusSettings torus() {
        return new TorusSettings(LoopSpans.ofWidth(OVERWORLD_CHUNK_WIDTH), NETHER_SCALE,
                LoopSpans.ofWidth(END_CHUNK_WIDTH), CylinderSettings.DEFAULT.generationOptions());
    }

    private static CarriedShape carriedShapeOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.carriedShapeOf(dimensions, key);
        assertNotNull(carried, key.identifier().toString());
        return carried;
    }
}
