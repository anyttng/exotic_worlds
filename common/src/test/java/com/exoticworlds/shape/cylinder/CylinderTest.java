package com.exoticworlds.shape.cylinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.DimensionContext;

import net.minecraft.core.Direction;
import net.minecraft.world.level.dimension.LevelStem;

class CylinderTest {

    @Test
    void theAxisAndTheWidthStateTheSpans() {
        ShapeOptions size = ShapeOptions.DEFAULT.with(CylinderParameters.SIZE, new CylinderSize(Direction.Axis.Z, 48));

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.Z, 48), Cylinder.SHAPE.spans(context(size)));
    }

    @Test
    void theSpansGiveTheirAxisAndWidthBack() {
        FlatShape alongZ = ShapeFixtures.cylinder(Direction.Axis.Z, 48);
        ShapeOptions size = Cylinder.SHAPE.optionsFor(alongZ).orElseThrow();

        assertEquals(Direction.Axis.Z, size.get(CylinderParameters.SIZE).axis());
        assertEquals(alongZ, Cylinder.SHAPE.spans(context(size)));
    }

    @Test
    void aPresetKeepsTheAxis() {
        ShapeOptions size = Cylinder.SHAPE.optionsFor(ShapeFixtures.cylinder(Direction.Axis.Z, 48)).orElseThrow();

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.Z, 256),
                Cylinder.SHAPE.spans(context(Cylinder.SHAPE.withPreset(WorldLoopPresets.LARGE, size))));
    }

    @Test
    void aTorusIsNotACylinder() {
        assertTrue(Cylinder.SHAPE.optionsFor(ShapeFixtures.torus(48)).isEmpty());
    }

    @Test
    void theSummaryStatesTheAxisAndTheWidth() {
        String summary = Cylinder.SHAPE.describe(
                Cylinder.SHAPE.optionsFor(ShapeFixtures.cylinder(Direction.Axis.Z, 48)).orElseThrow()).getString();

        assertTrue(summary.endsWith(" Z, 48"));
    }

    @Test
    void theEndStartsFromItsOwnDefaultWidth() {
        assertEquals(new CylinderSize(Direction.Axis.X, WorldLoopSizes.END_DEFAULT_WIDTH_CHUNKS),
                Cylinder.SHAPE.defaultOptions(LevelStem.END).get(CylinderParameters.SIZE));
    }

    private static DimensionContext context(ShapeOptions options) {
        return new DimensionContext(LevelStem.OVERWORLD, options, null, 1.0, 1.0);
    }
}
