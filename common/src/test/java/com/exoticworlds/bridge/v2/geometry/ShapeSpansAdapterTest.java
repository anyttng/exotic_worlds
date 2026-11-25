package com.exoticworlds.bridge.v2.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.core.Direction;

class ShapeSpansAdapterTest {

    @Test
    void aShapeComesBackEqualThroughItsSpans() {
        for (FlatShape shape : List.of(ShapeFixtures.latticeTorus(64, 128, 8),
                ShapeFixtures.cylinder(Direction.Axis.Z, 48), FlatShape.rectangle())) {
            assertEquals(shape, ShapeSpansAdapter.flatShape(ShapeSpansAdapter.loopSpans(shape)), shape.toString());
        }
    }

    @Test
    void theSpansOfAShapeStateItsBoundsAndSkew() {
        assertEquals(LoopSpansV2.ofWidths(64, 128).withSkew(8),
                ShapeSpansAdapter.loopSpans(ShapeFixtures.latticeTorus(64, 128, 8)));
    }

    @Test
    void aMirroredShapeHasNoSpans() {
        FlatShape mirrored = ShapeFixtures.mirrored(WorldLoopBounds.ofWidths(64, 64), Direction.Axis.X, 0);

        assertThrows(IllegalArgumentException.class, () -> ShapeSpansAdapter.loopSpans(mirrored));
    }
}
