package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.WorldCopies;

class JourneyMapFoldLapsTest {
    private static final int TILE = 512;
    private static final ToroidalShape WIDE = MapShapes.torus(-937, 938);
    private static final int[] EDGE_X = {14024, 15944};
    private static final int[] MIDDLE_Z = {-960, 960};

    @Test
    void aTileAcrossTheEdgeIsDrawnOnTheCopyTheViewReaches() {
        List<WorldCopies.Copy> drawn = JourneyMapFold.drawnCopies(WIDE, 8, EDGE_X, MIDDLE_Z, MapCopies.REPEATED);
        assertEquals(List.of(new WorldCopies.Copy(30000, 0)),
                JourneyMapFold.tileCopies(drawn, -15360, 0, TILE, EDGE_X, MIDDLE_Z),
                "region -30 one world over covers 14640..15151, inside the view; where it is, it is 30000 blocks away");
    }

    @Test
    void aTileInsideTheViewTakesNoCopyBesideItself() {
        List<WorldCopies.Copy> drawn = JourneyMapFold.drawnCopies(WIDE, 8, EDGE_X, MIDDLE_Z, MapCopies.REPEATED);
        assertEquals(List.of(), JourneyMapFold.tileCopies(drawn, 14848, 0, TILE, EDGE_X, MIDDLE_Z));
    }

    @Test
    void onlyTheDrawnCopiesReachATile() {
        assertEquals(List.of(), JourneyMapFold.tileCopies(List.of(WorldCopies.IDENTITY), -15360, 0, TILE, EDGE_X, MIDDLE_Z),
                "the world alone drawn left region -30 a copy");
    }

    @Test
    void aViewWiderThanTheWorldTakesEveryCopyTheTileMeets() {
        int[] span = {-1000, 1000};
        List<WorldCopies.Copy> drawn = JourneyMapFold.drawnCopies(MapShapes.torus(-16, 16), 1, span, span, MapCopies.REPEATED);
        assertEquals(15, JourneyMapFold.tileCopies(drawn, 0, 0, TILE, span, span).size(),
                "region 0 meets -1000..999 one and two worlds back and one on, on both axes, less itself");
    }

    @Test
    void aTileOfALatticeTorusIsDrawnOnTheSkewedRowAbove() {
        int[] spanX = {-256, 256};
        int[] spanZ = {256, 768};
        List<WorldCopies.Copy> drawn = JourneyMapFold.drawnCopies(MapShapes.latticeTorus(-16, 16, 16), 1, spanX, spanZ,
                MapCopies.REPEATED);
        assertEquals(Set.of(new WorldCopies.Copy(-256, 512)),
                new HashSet<>(JourneyMapFold.tileCopies(drawn, 0, -256, 256, spanX, spanZ)),
                "the tile at 0..255 does not show on the row above at -256..-1, skewed by 256");
    }
}
