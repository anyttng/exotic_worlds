package com.exoticworlds.bridge.v1.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.OptionValues;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.Direction;

class ShapeBridgeTest {

    @Test
    void theFlatShapeOfSpansCarriesTheSkew() {
        assertEquals(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(64, 128), 8),
                ShapeBridge.flatShapeOf(LoopSpans.ofWidths(64, 128).withSkew(8)));
        assertEquals(ShapeFixtures.torus(WorldLoopBounds.ofWidths(64, 128)),
                ShapeBridge.flatShapeOf(LoopSpans.ofWidths(64, 128)));
    }

    @Test
    void spansAtTheStatedFloorAreCarriedAndSpansUnderItAreRefused() {
        int floor = LoopSpans.minChunkWidth();

        new CarriedShape(ShapeBridge.flatShapeOf(LoopSpans.ofWidth(floor)), OptionValues.DEFAULT);
        new CarriedShape(ShapeBridge.flatShapeOf(LoopSpans.ofWidth(Direction.Axis.Z, floor)), OptionValues.DEFAULT);
        assertThrows(IllegalArgumentException.class,
                () -> new CarriedShape(ShapeBridge.flatShapeOf(LoopSpans.ofWidth(floor - 2)), OptionValues.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> new CarriedShape(
                ShapeBridge.flatShapeOf(LoopSpans.ofWidths(floor, floor - 2)), OptionValues.DEFAULT));
    }
}
