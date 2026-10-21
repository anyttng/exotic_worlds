package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class AtlasCenterSeatTest {
    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    @Test
    void aCenterSeatsOnTheCopyNearestThePoint() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)));
        assertEquals(256, AtlasCenterSeat.toward(fold, Direction.Axis.X, -256, 0, 300.0, 0.0),
                "the map on the lower bound sits a lap up beside a point past the upper seam");
        assertEquals(-256, AtlasCenterSeat.toward(fold, Direction.Axis.X, -256, 0, -200.0, 0.0),
                "a point inside the world keeps the canonical center");
        assertEquals(-512, AtlasCenterSeat.toward(fold, Direction.Axis.Z, 0, 0, 0.0, -600.0),
                "Z seats the same way below the lower seam");
    }

    @Test
    void bothAxesSeatTogetherOnASkewedTorus() {
        WorldFold fold = new DeckGroupFold(FlatShape.latticeTorus(new WorldLoopBounds(TINY, TINY), 8),
                GenerationOptions.DEFAULT);
        assertEquals(512, AtlasCenterSeat.toward(fold, Direction.Axis.Z, 0, 0, 0.0, 500.0),
                "the center a lap up in Z");
        assertEquals(128, Math.abs(AtlasCenterSeat.toward(fold, Direction.Axis.X, 0, 0, 0.0, 500.0)),
                "the same seat moves X by the skew");
    }

    @Test
    void aCylinderLeavesItsUnboundedAxis() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.cylinder(new WorldLoopBounds(TINY, AxisBounds.Unbounded.INSTANCE)));
        assertEquals(512, AtlasCenterSeat.toward(fold, Direction.Axis.X, 0, 4000, 500.0, -4000.0),
                "X seats a lap up");
        assertEquals(4000, AtlasCenterSeat.toward(fold, Direction.Axis.Z, 0, 4000, 500.0, -4000.0),
                "Z keeps the center however far the point is");
    }

    @Test
    void anUnwrappedLevelKeepsTheCenter() {
        assertEquals(-256, AtlasCenterSeat.toward(null, Direction.Axis.X, -256, 0, 300.0, 0.0),
                "no fold, no seat");
    }
}
