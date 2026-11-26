package com.exoticworlds.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.NumberRange;
import com.exoticworlds.shape.option.OptionPart;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class TorusTest {
    private static final ResourceKey<LevelStem> ISLANDS =
            ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath("example_mod", "islands"));

    private static final boolean IS_END_TERRAIN = true;


    @Test
    void theSizeStatesTheSpans() {
        ShapeOptions size = ShapeOptions.DEFAULT.with(TorusParameters.SIZE, new TorusSize(48, 64, 4));

        assertEquals(ShapeFixtures.latticeTorus(48, 64, 4), Torus.SHAPE.spans(context(size)));
    }

    @Test
    void theSpansGiveTheirSizeBackWithTheShiftInTheFieldRange() {
        FlatShape skewed = ShapeFixtures.latticeTorus(48, 64, -4);
        ShapeOptions size = Torus.SHAPE.optionsFor(skewed).orElseThrow();

        assertEquals(44, size.get(TorusParameters.SIZE).shiftChunks());
        assertEquals(skewed, Torus.SHAPE.spans(context(size)));
    }

    @Test
    void aPresetMakesTheTorusSquareAndUnskewed() {
        ShapeOptions size = Torus.SHAPE.optionsFor(ShapeFixtures.latticeTorus(48, 64, 4)).orElseThrow();

        assertEquals(ShapeFixtures.torus(256),
                Torus.SHAPE.spans(context(Torus.SHAPE.withPreset(WorldLoopPresets.LARGE, size))));
    }

    @Test
    void aCylinderIsNotATorus() {
        assertTrue(Torus.SHAPE.optionsFor(ShapeFixtures.cylinder(Direction.Axis.X, 48)).isEmpty());
    }

    @Test
    void theSummaryStatesBothWidthsAndAShiftOnlyWhereThereIsOne() {
        String plain = Torus.SHAPE.describe(Torus.SHAPE.optionsFor(ShapeFixtures.torus(128)).orElseThrow()).getString();
        String shifted = Torus.SHAPE.describe(
                Torus.SHAPE.optionsFor(ShapeFixtures.latticeTorus(48, 64, 4)).orElseThrow()).getString();

        assertTrue(plain.endsWith(", 128 × 128"));
        assertFalse(plain.contains("summary.shift"));
        assertTrue(shifted.contains(", 48 × 64, "));
        assertTrue(shifted.contains("summary.shift"));
    }

    @Test
    void theEndStartsFromItsOwnDefaultWidth() {
        int endWidthChunks = WorldLoopSizes.END_DEFAULT_WIDTH_CHUNKS;

        assertEquals(new TorusSize(endWidthChunks, endWidthChunks, FlatShape.NO_SKEW),
                Torus.SHAPE.defaultOptions(StemTerrain.ofKey(LevelStem.END)).get(TorusParameters.SIZE));
    }

    @Test
    void aWidthStartsHigherInTheEnd() {
        OptionPart.WholeNumber<TorusSize> xWidth = number(row(sizeParameter().getParts().get(0)).parts().get(0));
        TorusSize size = new TorusSize(48, 64, 0);

        assertEquals(WorldLoopSizes.MIN_WIDTH_CHUNKS,
                xWidth.range().apply(StemTerrain.ofKey(LevelStem.OVERWORLD), size).min());
        assertEquals(WorldLoopSizes.END_MIN_WIDTH_CHUNKS,
                xWidth.range().apply(StemTerrain.ofKey(LevelStem.END), size).min());
    }

    @Test
    void aModStemWithEndTerrainTakesTheEndFloorAndTheEndDefault() {
        OptionPart.WholeNumber<TorusSize> xWidth = number(row(sizeParameter().getParts().get(0)).parts().get(0));
        StemTerrain islands = new StemTerrain(ISLANDS, IS_END_TERRAIN);

        assertEquals(WorldLoopSizes.END_MIN_WIDTH_CHUNKS, xWidth.range().apply(islands, new TorusSize(48, 64, 0)).min());
        assertEquals(TorusSize.square(WorldLoopSizes.END_DEFAULT_WIDTH_CHUNKS),
                Torus.SHAPE.defaultOptions(islands).get(TorusParameters.SIZE));
        assertEquals(WorldLoopSizes.MIN_WIDTH_CHUNKS,
                xWidth.range().apply(new StemTerrain(ISLANDS, !IS_END_TERRAIN), new TorusSize(48, 64, 0)).min());
    }

    @Test
    void theShiftRunsBelowTheXWidth() {
        OptionPart.WholeNumber<TorusSize> shift = number(sizeParameter().getParts().get(1));

        assertEquals(new NumberRange(0, 47),
                shift.range().apply(StemTerrain.ofKey(LevelStem.OVERWORLD), new TorusSize(48, 64, 0)));
    }

    @SuppressWarnings("unchecked")
    private static DimensionOption<TorusSize> sizeParameter() {
        return (DimensionOption<TorusSize>) Torus.SHAPE.getParameters().get(0);
    }

    private static OptionPart.Row<TorusSize> row(OptionPart<TorusSize> part) {
        return (OptionPart.Row<TorusSize>) part;
    }

    private static OptionPart.WholeNumber<TorusSize> number(OptionPart<TorusSize> part) {
        return (OptionPart.WholeNumber<TorusSize>) part;
    }

    private static DimensionContext context(ShapeOptions options) {
        return new DimensionContext(LevelStem.OVERWORLD, options, null, 1.0, 1.0);
    }
}
