package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.WorldCopies;

class XaeroWorldMapFoldLapsTest {
    private static final ToroidalShape TORUS = MapShapes.torus(-32, 32);
    private static final int[] INSIDE_Z = {-100, 100};

    @Test
    void aRepeatedMapDrawsEveryCopyTheViewTouches() {
        assertEquals(Set.of(new WorldCopies.Copy(-1024, 0), WorldCopies.IDENTITY, new WorldCopies.Copy(1024, 0)),
                new HashSet<>(XaeroWorldMapFold.drawnCopies(TORUS, new int[] {-1536, 1536}, INSIDE_Z, MapCopies.REPEATED)),
                "three worlds in view are not three copies");
        assertEquals(List.of(new WorldCopies.Copy(3072, 0)),
                XaeroWorldMapFold.drawnCopies(TORUS, new int[] {3000, 3100}, INSIDE_Z, MapCopies.REPEATED),
                "a view three worlds over does not draw that copy");
    }

    @Test
    void aSingleCopyMapDrawsTheWorldAlone() {
        assertEquals(List.of(WorldCopies.IDENTITY),
                XaeroWorldMapFold.drawnCopies(TORUS, new int[] {-1536, 1536}, INSIDE_Z, MapCopies.SINGLE),
                "the copies beside the world were drawn under SINGLE");
        assertEquals(List.of(WorldCopies.IDENTITY),
                XaeroWorldMapFold.drawnCopies(TORUS, new int[] {500, 600}, INSIDE_Z, MapCopies.SINGLE),
                "a view across the seam at 512 lost the world or kept the copy past it");
    }

    @Test
    void aSingleCopyMapDrawsNothingForAViewPastTheWorld() {
        assertEquals(List.of(), XaeroWorldMapFold.drawnCopies(TORUS, new int[] {3000, 3100}, INSIDE_Z, MapCopies.SINGLE),
                "a view three worlds over drew a copy under SINGLE");
    }

    @Test
    void anUnshapedWorldDrawsItselfInBothModes() {
        int[] far = {-40000000, 40000000};
        assertEquals(List.of(WorldCopies.IDENTITY), XaeroWorldMapFold.drawnCopies(null, far, far, MapCopies.SINGLE),
                "an unshaped world lost itself under SINGLE");
    }

    @Test
    void aRepeatedLatticeMapDrawsTheRowAboveMovedByTheSkew() {
        assertEquals(Set.of(new WorldCopies.Copy(-256, 512), new WorldCopies.Copy(256, 512)),
                new HashSet<>(XaeroWorldMapFold.drawnCopies(MapShapes.latticeTorus(-16, 16, 16),
                        new int[] {-256, 256}, new int[] {256, 768}, MapCopies.REPEATED)),
                "the row above a skew-256 world is not drawn at -256 and 256");
    }
}
