package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AtlasViewTest {
    @Test
    void theZoomFloorLetsTheWorldCoverTheView() {
        assertEquals(0.921875F, AtlasView.zoomCovering(236.0F, 512, 128, 64.0F));
    }

    @Test
    void aZoomChangeKeepsTheMapUnderTheViewCentre() {
        assertEquals(-40.0, AtlasView.panKeepingCentre(10.0, 100.0F, 20.0F, 64.0F, 0.0F, 128.0F));
    }

    @Test
    void aWorldThatLeavesTheViewIsPulledBackInside() {
        assertEquals(-10.0F, AtlasView.panInside(10.0F, 100.0F, 0.0F, 100.0F));
        assertEquals(50.0F, AtlasView.panInside(-50.0F, 100.0F, 0.0F, 100.0F));
        assertEquals(0.0F, AtlasView.panInside(-10.0F, 120.0F, 0.0F, 100.0F));
    }

    @Test
    void aWorldPulledInsideStaysWhereItWasPut() {
        for (float worldLength : new float[] {60.0F, 100.0F, 140.0F}) {
            float worldStart = 30.0F;
            worldStart += AtlasView.panInside(worldStart, worldLength, 0.0F, 100.0F);
            assertEquals(0.0F, AtlasView.panInside(worldStart, worldLength, 0.0F, 100.0F), "world " + worldLength);
        }
    }
}
