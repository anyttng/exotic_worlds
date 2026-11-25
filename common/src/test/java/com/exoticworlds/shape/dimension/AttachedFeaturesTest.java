package com.exoticworlds.shape.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.feature.DimensionScope;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;
import com.exoticworlds.shape.feature.spawnpoint.ChosenSpawn;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.OptionPart;
import com.exoticworlds.shape.option.ScopedOption;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class AttachedFeaturesTest {
    private static final ResourceKey<LevelStem> MOD_DIMENSION = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("example_mod", "sky"));

    private static final Identifier NOBODYS_SHAPE = Identifier.fromNamespaceAndPath("example_mod", "nobodys_shape");

    @BeforeAll
    static void registerOptions() {
        StartupRegistrations.options();
    }

    @Test
    void eachShapeHoldsItsOwnOptionsInTheOrderTheyAreAttached() {
        assertEquals(List.of(ChosenSpawn.FEATURE.getShown(), CompactBiomes.FEATURE.getShown(),
                GuaranteedLand.FEATURE.getShown(), GuaranteedNetherComplexes.FEATURE.getShown()),
                optionsOf(DeclaredShapes.TORUS));
        assertEquals(List.of(ChosenSpawn.FEATURE.getShown(), CompactBiomes.FEATURE.getShown()),
                optionsOf(DeclaredShapes.CYLINDER));
    }

    @Test
    void aShapeNothingIsAttachedToHasNoOptions() {
        assertEquals(List.of(), AttachedFeatures.optionsOf(NOBODYS_SHAPE));
    }

    @Test
    void anOptionIsOfferedOnlyInTheDimensionsOfItsScope() {
        assertTrue(scopeOf(DeclaredShapes.TORUS, CompactBiomes.FEATURE.getShown()).contains(MOD_DIMENSION));
        assertTrue(scopeOf(DeclaredShapes.CYLINDER, CompactBiomes.FEATURE.getShown()).contains(LevelStem.END));

        assertTrue(scopeOf(DeclaredShapes.TORUS, GuaranteedLand.FEATURE.getShown()).contains(LevelStem.OVERWORLD));
        assertFalse(scopeOf(DeclaredShapes.TORUS, GuaranteedLand.FEATURE.getShown()).contains(LevelStem.NETHER));

        assertTrue(scopeOf(DeclaredShapes.TORUS, GuaranteedNetherComplexes.FEATURE.getShown())
                .contains(LevelStem.NETHER));
        assertFalse(scopeOf(DeclaredShapes.TORUS, GuaranteedNetherComplexes.FEATURE.getShown())
                .contains(LevelStem.OVERWORLD));
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

    private static List<DimensionOption<?>> optionsOf(Identifier shape) {
        return AttachedFeatures.optionsOf(shape).stream().<DimensionOption<?>>map(ScopedOption::option).toList();
    }

    private static DimensionScope scopeOf(Identifier shape, DimensionOption<?> option) {
        return AttachedFeatures.optionsOf(shape).stream()
                .filter(scoped -> scoped.option() == option)
                .findFirst()
                .orElseThrow()
                .scope();
    }
}
