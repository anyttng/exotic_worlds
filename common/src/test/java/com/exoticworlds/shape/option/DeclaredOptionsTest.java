package com.exoticworlds.shape.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DeclaredOptionsTest {
    private static final Identifier MOD_SHAPE = Identifier.fromNamespaceAndPath("example_mod", "band");

    private static final ResourceKey<LevelStem> MOD_DIMENSION = ResourceKey.create(Registries.LEVEL_STEM,
            Identifier.fromNamespaceAndPath("example_mod", "sky"));

    @Test
    void theModsOwnOptionsAreDeclaredInTheOrderOfTheirPositions() {
        StartupRegistrations.options();

        assertEquals(List.of(CompactBiomes.SHOWN, GuaranteedLand.SHOWN, GuaranteedNetherComplexes.SHOWN),
                DeclaredOptions.getAll());
    }

    @Test
    void theModsOwnOptionsNameTheirShapesAndDimensions() {
        assertTrue(CompactBiomes.SHOWN.isOfferedFor(DeclaredShapes.CYLINDER, LevelStem.END));
        assertTrue(CompactBiomes.SHOWN.isOfferedFor(MOD_SHAPE, MOD_DIMENSION));

        assertTrue(GuaranteedLand.SHOWN.isOfferedFor(DeclaredShapes.TORUS, LevelStem.OVERWORLD));
        assertFalse(GuaranteedLand.SHOWN.isOfferedFor(DeclaredShapes.CYLINDER, LevelStem.OVERWORLD));
        assertFalse(GuaranteedLand.SHOWN.isOfferedFor(DeclaredShapes.TORUS, LevelStem.NETHER));
        assertFalse(GuaranteedLand.SHOWN.isOfferedFor(MOD_SHAPE, LevelStem.OVERWORLD));

        assertTrue(GuaranteedNetherComplexes.SHOWN.isOfferedFor(DeclaredShapes.TORUS, LevelStem.NETHER));
        assertFalse(GuaranteedNetherComplexes.SHOWN.isOfferedFor(DeclaredShapes.TORUS, LevelStem.OVERWORLD));
    }

    @Test
    void compactBiomesIsAChoiceOfModeWithAFactorShownForCustomAlone() {
        List<OptionPart<ClimateScale>> parts = CompactBiomes.SHOWN.getParts();
        OptionPart.WholeNumber<ClimateScale> factor = (OptionPart.WholeNumber<ClimateScale>) parts.get(1);

        assertEquals(2, parts.size());
        assertTrue(parts.get(0).shownWhen().test(ClimateScale.AUTO));
        assertFalse(factor.shownWhen().test(ClimateScale.AUTO));
        assertTrue(factor.shownWhen().test(ClimateScale.custom(6)));
        assertEquals(6, factor.read().apply(ClimateScale.custom(6)));
        assertEquals(ClimateScale.custom(9), factor.write().apply(ClimateScale.custom(6), 9));
    }
}
