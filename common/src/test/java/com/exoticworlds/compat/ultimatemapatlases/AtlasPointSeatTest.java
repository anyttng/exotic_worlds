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

import net.minecraft.world.phys.Vec3;

class AtlasPointSeatTest {
    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    @Test
    void aPointSeatsOnTheCopyNearestTheAnchor() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(TINY, TINY)));
        assertEquals(new Vec3(262.0, 70.0, 10.0), AtlasPointSeat.toward(fold, new Vec3(300.0, 70.0, 0.0), -250.0, 10.0),
                "a marker beside the lower bound sits a lap up beside a player past the upper seam");
        assertEquals(new Vec3(-200.0, 0.0, 0.0), AtlasPointSeat.toward(fold, new Vec3(-180.0, 0.0, 0.0), -200.0, 0.0),
                "a marker near the anchor inside the world keeps its point");
        assertEquals(new Vec3(0.0, 0.0, -600.0), AtlasPointSeat.toward(fold, new Vec3(0.0, 0.0, -580.0), 0.0, -88.0),
                "Z seats the same way below the lower seam");
    }

    @Test
    void bothAxesSeatTogetherOnASkewedTorus() {
        WorldFold fold = new DeckGroupFold(FlatShape.latticeTorus(new WorldLoopBounds(TINY, TINY), 8),
                GenerationOptions.DEFAULT);
        Vec3 seated = AtlasPointSeat.toward(fold, new Vec3(0.0, 0.0, 500.0), 0.0, 0.0);
        assertEquals(512.0, seated.z, "the point a lap up in Z");
        assertEquals(128.0, Math.abs(seated.x), "the same seat moves X by the skew");
    }

    @Test
    void aCylinderLeavesItsUnboundedAxis() {
        WorldFold fold = WorldFolds.of(
                ShapeFixtures.cylinder(new WorldLoopBounds(TINY, AxisBounds.Unbounded.INSTANCE)));
        assertEquals(new Vec3(512.0, 0.0, 4000.0),
                AtlasPointSeat.toward(fold, new Vec3(500.0, 0.0, -4000.0), 0.0, 4000.0),
                "X seats a lap up and Z keeps the point however far the anchor is");
    }

    @Test
    void anUnwrappedLevelKeepsThePoint() {
        assertEquals(new Vec3(-250.0, 5.0, 10.0),
                AtlasPointSeat.toward((WorldFold) null, new Vec3(300.0, 5.0, 0.0), -250.0, 10.0), "no fold, no seat");
    }
}
