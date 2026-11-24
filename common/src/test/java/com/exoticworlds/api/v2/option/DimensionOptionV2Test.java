package com.exoticworlds.api.v2.option;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.shape.BuiltInShapesV2;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DimensionOptionV2Test {
    private static final Identifier MOD_SHAPE = Identifier.fromNamespaceAndPath("example_mod", "band");

    private static final ResourceKey<LevelStem> MOD_DIMENSION = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("example_mod", "sky"));

    private static final ShapeOptionV2<Integer> FLOOR = new ShapeOptionV2<>("example_floor", 100, Codec.INT, 0);

    private static final DimensionOptionV2<Integer> EVERYWHERE =
            DimensionOptionV2.of(FLOOR, "gui.example_mod.floor", List.of(OptionPartV2.number(0, 64)));

    @Test
    void aDeclarationWithNoListsIsOfferedForEveryShapeOnEveryDimension() {
        assertTrue(EVERYWHERE.isOfferedFor(BuiltInShapesV2.TORUS, LevelStem.OVERWORLD));
        assertTrue(EVERYWHERE.isOfferedFor(BuiltInShapesV2.CYLINDER, LevelStem.END));
        assertTrue(EVERYWHERE.isOfferedFor(MOD_SHAPE, MOD_DIMENSION));
    }

    @Test
    void aNamedDimensionNarrowsItToThatDimension() {
        DimensionOptionV2<Integer> onTheModDimension = EVERYWHERE.forDimensions(List.of(MOD_DIMENSION));

        assertTrue(onTheModDimension.isOfferedFor(BuiltInShapesV2.TORUS, MOD_DIMENSION));
        assertFalse(onTheModDimension.isOfferedFor(BuiltInShapesV2.TORUS, LevelStem.OVERWORLD));
    }

    @Test
    void aNamedShapeNarrowsItToThatShapeAModsOwnIncluded() {
        DimensionOptionV2<Integer> forTheBand = EVERYWHERE.forShapes(List.of(MOD_SHAPE, BuiltInShapesV2.CYLINDER));

        assertTrue(forTheBand.isOfferedFor(MOD_SHAPE, LevelStem.NETHER));
        assertTrue(forTheBand.isOfferedFor(BuiltInShapesV2.CYLINDER, LevelStem.NETHER));
        assertFalse(forTheBand.isOfferedFor(BuiltInShapesV2.TORUS, LevelStem.NETHER));
    }

    @Test
    void aNumberPartRefusesBoundsOutOfOrder() {
        assertThrows(IllegalArgumentException.class, () -> OptionPartV2.number(8, 2));
    }
}
