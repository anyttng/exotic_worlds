package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class MapCenterSeatTest {
    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    @Test
    void anUnwrappedLevelKeepsTheIncomingCenter() {
        assertEquals(-200, MapCenterSeat.toward(null, Direction.Axis.X, -200, 0, 300.0, 0.0),
                "no fold, the center as it came in");
        assertEquals(40, MapCenterSeat.toward(null, Direction.Axis.Z, 0, 40, 0.0, -600.0),
                "Z the same");
    }

    @Test
    void aWrappedLevelSeatsTheIncomingCenter() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)));
        assertEquals(256, MapCenterSeat.toward(fold, Direction.Axis.X, -256, 0, 300.0, 0.0),
                "the map on the lower bound sits a lap up beside a point past the upper seam");
    }
}
