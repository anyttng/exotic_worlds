package com.exoticworlds.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.dimension.DimensionContext;

import net.minecraft.world.level.dimension.LevelStem;

class TorusTest {

    @Test
    void theSizeOptionsStateTheSpans() {
        ShapeOptions size = ShapeOptions.DEFAULT.with(Torus.X_WIDTH, 48).with(Torus.Z_WIDTH, 64).with(Torus.SHIFT, 4);

        assertEquals(ShapeFixtures.latticeTorus(48, 64, 4), Torus.SHAPE.spans(context(size)));
    }

    @Test
    void theSpansGiveTheirSizeBackWithTheShiftInTheFieldRange() {
        FlatShape skewed = ShapeFixtures.latticeTorus(48, 64, -4);
        ShapeOptions size = Torus.SHAPE.optionsFor(skewed);

        assertEquals(44, size.get(Torus.SHIFT));
        assertEquals(skewed, Torus.SHAPE.spans(context(size)));
    }

    @Test
    void aPresetMakesTheTorusSquareAndUnskewed() {
        ShapeOptions size = Torus.SHAPE.optionsFor(ShapeFixtures.latticeTorus(48, 64, 4));

        assertEquals(ShapeFixtures.torus(256), Torus.SHAPE.spans(context(Torus.SHAPE.withPreset(256, size))));
    }

    private static DimensionContext context(ShapeOptions options) {
        return new DimensionContext(LevelStem.OVERWORLD, options, null, 1.0, 1.0);
    }
}
