package com.exoticworlds.bridge.v1.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopBounds;

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

        new CarriedShape(null, ShapeBridge.flatShapeOf(LoopSpans.ofWidth(floor)), ShapeOptions.DEFAULT);
        new CarriedShape(null, ShapeBridge.flatShapeOf(LoopSpans.ofWidth(Direction.Axis.Z, floor)), ShapeOptions.DEFAULT);
        assertThrows(IllegalArgumentException.class,
                () -> new CarriedShape(null, ShapeBridge.flatShapeOf(LoopSpans.ofWidth(floor - 2)), ShapeOptions.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> new CarriedShape(null, 
                ShapeBridge.flatShapeOf(LoopSpans.ofWidths(floor, floor - 2)), ShapeOptions.DEFAULT));
    }
}
