package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;

class XaeroSlotSourceTest {
    private static final ToroidalShape SMALL = MapShapes.torus(-16, 16);

    @Test
    void anUnshapedWorldKeepsXaerosTextureAndHidesOurCandidate() {
        assertEquals(XaeroSlotSource.RAW, source(null, MapCopies.REPEATED, false, false, true, 256, 0));
        assertEquals(XaeroSlotSource.HIDDEN, source(null, MapCopies.REPEATED, true, false, true, 256, 0));
    }

    @Test
    void aGlueableSlotInsideTheWorldKeepsItsOwnTexture() {
        assertEquals(XaeroSlotSource.RAW, source(SMALL, MapCopies.REPEATED, false, true, true, 0, 0));
        assertEquals(XaeroSlotSource.HIDDEN, source(SMALL, MapCopies.REPEATED, true, true, true, 0, 0),
                "a candidate region drawn in place of a missing one shows nothing where nothing folds");
    }

    @Test
    void aGlueableSlotPastTheSeamReadsTheCanonicalSlot() {
        assertEquals(new XaeroSlotSource(XaeroSlotSource.Kind.CANONICAL, true, -256, 0),
                source(SMALL, MapCopies.REPEATED, false, true, false, 256, 0), "256 folds onto -256");
    }

    @Test
    void aGlueableSlotPastALatticeSeamReadsTheSlotTheSkewMovesItTo() {
        assertEquals(new XaeroSlotSource(XaeroSlotSource.Kind.CANONICAL, true, -256, -256),
                source(MapShapes.latticeTorus(-16, 16, 16), MapCopies.REPEATED, false, true, false, 0, 256));
    }

    @Test
    void aSingleCopyViewLeavesASlotPastTheSeamEmpty() {
        assertEquals(XaeroSlotSource.FOLDED_HIDDEN, source(SMALL, MapCopies.SINGLE, false, true, false, 256, 0));
    }

    @Test
    void aSlotOffTheGlueGridDrawsThroughItsClippedCopies() {
        assertEquals(XaeroSlotSource.RAW, source(SMALL, MapCopies.REPEATED, false, false, true, 0, 0));
        assertEquals(XaeroSlotSource.ANY_CANONICAL, source(SMALL, MapCopies.REPEATED, false, false, false, 224, 0));
        assertEquals(XaeroSlotSource.ANY_CANONICAL, source(SMALL, MapCopies.REPEATED, true, false, true, 0, 0));
    }

    private static XaeroSlotSource source(ToroidalShape shape, MapCopies mapCopies, boolean isCandidate, boolean glueable,
            boolean insideWorld, int viewBlockX, int viewBlockZ) {
        return XaeroSlotSource.of(shape, mapCopies, isCandidate, glueable, insideWorld, viewBlockX, viewBlockZ);
    }
}
