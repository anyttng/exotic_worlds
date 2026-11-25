package com.exoticworlds.shape.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.cylinder.Cylinder;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DeclaredOptionsTest {
    private static final ResourceKey<LevelStem> MOD_DIMENSION = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("example_mod", "sky"));

    @BeforeAll
    static void registerOptions() {
        StartupRegistrations.options();
    }

    @Test
    void eachShapeHoldsItsOwnOptionsInTheOrderTheyAreRegistered() {
        assertEquals(List.of(CompactBiomes.FEATURE.getShown(), GuaranteedLand.FEATURE.getShown(), GuaranteedNetherComplexes.FEATURE.getShown()),
                optionsOf(Torus.SHAPE));
        assertEquals(List.of(CompactBiomes.FEATURE.getShown()), optionsOf(Cylinder.SHAPE));
    }

    @Test
    void anOptionIsOfferedOnlyInTheDimensionsOfItsScope() {
        assertTrue(scopeOf(Torus.SHAPE, CompactBiomes.FEATURE.getShown()).contains(MOD_DIMENSION));
        assertTrue(scopeOf(Cylinder.SHAPE, CompactBiomes.FEATURE.getShown()).contains(LevelStem.END));

        assertTrue(scopeOf(Torus.SHAPE, GuaranteedLand.FEATURE.getShown()).contains(LevelStem.OVERWORLD));
        assertFalse(scopeOf(Torus.SHAPE, GuaranteedLand.FEATURE.getShown()).contains(LevelStem.NETHER));

        assertTrue(scopeOf(Torus.SHAPE, GuaranteedNetherComplexes.FEATURE.getShown()).contains(LevelStem.NETHER));
        assertFalse(scopeOf(Torus.SHAPE, GuaranteedNetherComplexes.FEATURE.getShown()).contains(LevelStem.OVERWORLD));
    }

    @Test
    void compactBiomesIsAChoiceOfModeWithAFactorShownForCustomAlone() {
        List<OptionPart<ClimateScale>> parts = CompactBiomes.FEATURE.getShown().getParts();
        OptionPart.WholeNumber<ClimateScale> factor = (OptionPart.WholeNumber<ClimateScale>) parts.get(1);

        assertEquals(2, parts.size());
        assertTrue(parts.get(0).shownWhen().test(ClimateScale.AUTO));
        assertFalse(factor.shownWhen().test(ClimateScale.AUTO));
        assertTrue(factor.shownWhen().test(ClimateScale.custom(6)));
        assertEquals(6, factor.read().apply(ClimateScale.custom(6)));
        assertEquals(ClimateScale.custom(9), factor.write().apply(ClimateScale.custom(6), 9));
    }

    private static List<DimensionOption<?>> optionsOf(DimensionShape shape) {
        return shape.getOptions().stream().<DimensionOption<?>>map(ScopedOption::option).toList();
    }

    private static DimensionScope scopeOf(DimensionShape shape, DimensionOption<?> option) {
        return shape.getOptions().stream()
                .filter(scoped -> scoped.option() == option)
                .findFirst()
                .orElseThrow()
                .scope();
    }
}
