package com.exoticworlds.shape.cylinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.shape.dimension.DimensionContext;

import net.minecraft.core.Direction;
import net.minecraft.world.level.dimension.LevelStem;

class CylinderTest {

    @Test
    void theAxisAndTheWidthStateTheSpans() {
        ShapeOptions size = ShapeOptions.DEFAULT.with(Cylinder.AXIS, Direction.Axis.Z).with(Cylinder.WIDTH, 48);

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.Z, 48), Cylinder.SHAPE.spans(context(size)));
    }

    @Test
    void theSpansGiveTheirAxisAndWidthBack() {
        FlatShape alongZ = ShapeFixtures.cylinder(Direction.Axis.Z, 48);
        ShapeOptions size = Cylinder.SHAPE.optionsFor(alongZ);

        assertEquals(Direction.Axis.Z, size.get(Cylinder.AXIS));
        assertEquals(alongZ, Cylinder.SHAPE.spans(context(size)));
    }

    @Test
    void aPresetKeepsTheAxis() {
        ShapeOptions size = Cylinder.SHAPE.optionsFor(ShapeFixtures.cylinder(Direction.Axis.Z, 48));

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.Z, 256),
                Cylinder.SHAPE.spans(context(Cylinder.SHAPE.withPreset(256, size))));
    }

    private static DimensionContext context(ShapeOptions options) {
        return new DimensionContext(LevelStem.OVERWORLD, options, null, 1.0, 1.0);
    }
}
