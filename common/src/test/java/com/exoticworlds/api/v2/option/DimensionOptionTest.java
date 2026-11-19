package com.exoticworlds.api.v2.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v2.shape.BuiltInShapes;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DimensionOptionTest {
    private static final ResourceLocation MOD_SHAPE = ResourceLocation.fromNamespaceAndPath("example_mod", "band");

    private static final ResourceKey<LevelStem> MOD_DIMENSION = ResourceKey.create(Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath("example_mod", "sky"));

    private static final WorldOption<Integer> FLOOR = new WorldOption<>("example_floor", 100, Codec.INT, 0);

    private static final DimensionOption<Integer> EVERYWHERE =
            DimensionOption.of(FLOOR, "gui.example_mod.floor", List.of(OptionPart.number(0, 64)));

    @Test
    void aDeclarationWithNoListsIsOfferedForEveryShapeOnEveryDimension() {
        assertTrue(EVERYWHERE.offeredFor(BuiltInShapes.TORUS, LevelStem.OVERWORLD));
        assertTrue(EVERYWHERE.offeredFor(BuiltInShapes.CYLINDER, LevelStem.END));
        assertTrue(EVERYWHERE.offeredFor(MOD_SHAPE, MOD_DIMENSION));
    }

    @Test
    void aNamedDimensionNarrowsItToThatDimension() {
        DimensionOption<Integer> onTheModDimension = EVERYWHERE.forDimensions(List.of(MOD_DIMENSION));

        assertTrue(onTheModDimension.offeredFor(BuiltInShapes.TORUS, MOD_DIMENSION));
        assertFalse(onTheModDimension.offeredFor(BuiltInShapes.TORUS, LevelStem.OVERWORLD));
    }

    @Test
    void aNamedShapeNarrowsItToThatShapeAModsOwnIncluded() {
        DimensionOption<Integer> forTheBand = EVERYWHERE.forShapes(List.of(MOD_SHAPE, BuiltInShapes.CYLINDER));

        assertTrue(forTheBand.offeredFor(MOD_SHAPE, LevelStem.NETHER));
        assertTrue(forTheBand.offeredFor(BuiltInShapes.CYLINDER, LevelStem.NETHER));
        assertFalse(forTheBand.offeredFor(BuiltInShapes.TORUS, LevelStem.NETHER));
    }

    @Test
    void theModsOwnOptionsNameTheirShapesAndDimensions() {
        assertTrue(CompactBiomes.SHOWN.offeredFor(BuiltInShapes.CYLINDER, LevelStem.END));
        assertTrue(CompactBiomes.SHOWN.offeredFor(MOD_SHAPE, MOD_DIMENSION));

        assertTrue(GuaranteedLand.SHOWN.offeredFor(BuiltInShapes.TORUS, LevelStem.OVERWORLD));
        assertFalse(GuaranteedLand.SHOWN.offeredFor(BuiltInShapes.CYLINDER, LevelStem.OVERWORLD));
        assertFalse(GuaranteedLand.SHOWN.offeredFor(BuiltInShapes.TORUS, LevelStem.NETHER));
        assertFalse(GuaranteedLand.SHOWN.offeredFor(MOD_SHAPE, LevelStem.OVERWORLD));

        assertTrue(GuaranteedNetherComplexes.SHOWN.offeredFor(BuiltInShapes.TORUS, LevelStem.NETHER));
        assertFalse(GuaranteedNetherComplexes.SHOWN.offeredFor(BuiltInShapes.TORUS, LevelStem.OVERWORLD));
    }

    @Test
    void compactBiomesIsAChoiceOfModeWithAFactorShownForCustomAlone() {
        List<OptionPart<ClimateScale>> parts = CompactBiomes.SHOWN.parts();
        OptionPart.WholeNumber<ClimateScale> factor = (OptionPart.WholeNumber<ClimateScale>) parts.get(1);

        assertEquals(2, parts.size());
        assertTrue(parts.get(0).shownWhen().test(ClimateScale.AUTO));
        assertFalse(factor.shownWhen().test(ClimateScale.AUTO));
        assertTrue(factor.shownWhen().test(ClimateScale.custom(6)));
        assertEquals(6, factor.read().apply(ClimateScale.custom(6)));
        assertEquals(ClimateScale.custom(9), factor.write().apply(ClimateScale.custom(6), 9));
    }

    @Test
    void aNumberPartRefusesBoundsOutOfOrder() {
        assertThrows(IllegalArgumentException.class, () -> OptionPart.number(8, 2));
    }
}
