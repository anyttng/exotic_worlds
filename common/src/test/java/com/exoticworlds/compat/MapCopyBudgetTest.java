package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

class MapCopyBudgetTest {
    @Test
    void theCopiesAJourneyMapViewDrewPaintTheBoxAroundThem() {
        Copies copies = MapCopyBudget.painted(MapShapes.torus(-16, 16), List.of(WorldCopies.IDENTITY,
                new WorldCopies.Copy(-512, 0), new WorldCopies.Copy(512, 1024)));
        assertEquals(2, copies.reach(), "the farthest copy is two worlds along Z");
        assertEquals(-768, copies.painted().minX(), "one copy left of a world starting at -256");
        assertEquals(767, copies.painted().maxX(), "one copy right of a 512-block world, last block inclusive");
        assertEquals(-256, copies.painted().minZ(), "no copy below the world");
        assertEquals(1279, copies.painted().maxZ(), "two copies above");
    }

    @Test
    void anUnshapedWorldPaintsEverywhereAndReachesNoCopy() {
        Copies copies = MapCopyBudget.painted(null, List.of(WorldCopies.IDENTITY));
        assertEquals(0, copies.reach(), "an unshaped world reached a copy");
        assertEquals(Integer.MIN_VALUE, copies.painted().minX(), "an unshaped world was bounded");
        assertEquals(Integer.MAX_VALUE, copies.painted().maxZ(), "an unshaped world was bounded");
    }
}
