package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.WorldCopies;

class JourneyMapFoldCopyRangeTest {
    private static final int BLIT_BUDGET = 16384;
    private static final ToroidalShape TINY = MapShapes.torus(-16, 16);
    private static final int[] NEAR = {-1000, 1000};
    private static final int[] FAR = {-50_000, 50_000};

    @Test
    void theViewDrawsEveryCopyItTouches() {
        assertEquals(25, JourneyMapFold.drawnCopies(TINY, 1, NEAR, NEAR, MapCopies.REPEATED).size(),
                "-1000..999 touches copies -2..2 of a 512-block world on both axes");
    }

    @Test
    void theBudgetBindsOnlyWhenTheGridsTilesTimesTheCopiesOverrunIt() {
        List<WorldCopies.Copy> copies = JourneyMapFold.drawnCopies(TINY, 16, FAR, FAR, MapCopies.REPEATED);
        assertEquals(BLIT_BUDGET / 16, copies.size(), "16 tiles over 197 x 197 copies did not keep 1024 of them");
        assertEquals(WorldCopies.IDENTITY, copies.get(0), "the world itself was not kept first");
        assertTrue(copies.contains(new WorldCopies.Copy(1024, 0)), "a copy two worlds from the centre was dropped");
        assertFalse(copies.contains(new WorldCopies.Copy(512 * 97, 0)), "a copy at the view's edge outran a nearer one");
        assertEquals(197, JourneyMapFold.drawnCopies(MapShapes.cylinder(-16, 16), 16, FAR, FAR, MapCopies.REPEATED).size(),
                "16 tiles over 197 copies of a cylinder do not stay inside the budget");
    }

    @Test
    void aSingleCopyMapDrawsTheWorldAloneWhateverTheViewAsks() {
        assertEquals(List.of(WorldCopies.IDENTITY), JourneyMapFold.drawnCopies(TINY, 1, NEAR, NEAR, MapCopies.SINGLE),
                "a torus under SINGLE got copies");
    }

    @Test
    void aFewTilesOnAWideWorldStillGetTheCopyTheViewReaches() {
        assertEquals(Set.of(WorldCopies.IDENTITY, new WorldCopies.Copy(30000, 0)),
                new HashSet<>(JourneyMapFold.drawnCopies(MapShapes.torus(-937, 938), 8,
                        new int[] {14024, 15944}, new int[] {-960, 960}, MapCopies.REPEATED)),
                "8 tiles on a 60 x 60-region world, a view crossing the +X seam and none along Z");
    }

    @Test
    void theRowPastALatticeSeamIsDrawnMovedByTheSkew() {
        assertEquals(Set.of(new WorldCopies.Copy(-256, 512), new WorldCopies.Copy(256, 512)),
                new HashSet<>(JourneyMapFold.drawnCopies(MapShapes.latticeTorus(-16, 16, 16), 1,
                        new int[] {-256, 256}, new int[] {256, 768}, MapCopies.REPEATED)),
                "the row above a skew-256 world is not drawn at -256 and 256");
    }
}
