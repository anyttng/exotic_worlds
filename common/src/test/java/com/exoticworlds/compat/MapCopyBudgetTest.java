package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

class MapCopyBudgetTest {
    @Test
    void thePaintedBoxSpansEveryCopyDrawn() {
        Copies copies = MapCopyBudget.painted(AxisCopies.looped(0, 512), 1, AxisCopies.looped(0, 512), 2);
        assertEquals(2, copies.reach(), "the reach is the wider of the two ranges");
        assertEquals(-512, copies.painted().minX(), "one copy left of a world starting at 0");
        assertEquals(1023, copies.painted().maxX(), "one copy right of a 512-block world, last block inclusive");
        assertEquals(-1024, copies.painted().minZ(), "two copies below");
        assertEquals(1535, copies.painted().maxZ(), "two copies above");
    }

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

    @Test
    void anUnboundedAxisPaintsEverywhere() {
        Copies copies = MapCopyBudget.painted(AxisCopies.UNBOUNDED, 0, AxisCopies.looped(0, 512), 0);
        assertEquals(Integer.MIN_VALUE, copies.painted().minX(), "an unbounded axis was bounded");
        assertEquals(Integer.MAX_VALUE, copies.painted().maxX(), "an unbounded axis was bounded");
        assertEquals(0, copies.painted().minZ(), "the looped axis kept its own bounds");
        assertEquals(511, copies.painted().maxZ(), "the looped axis kept its own bounds");
    }
}
