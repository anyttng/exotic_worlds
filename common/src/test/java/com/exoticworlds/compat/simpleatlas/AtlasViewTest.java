package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AtlasViewTest {
    @Test
    void theContentAreaMatchesSimpleAtlasOnAFullHdScreen() {
        assertEquals(new AtlasView(606.0F, 324.0F, 708.0F, 429.0F), AtlasView.of(1920, 1080));
    }

    @Test
    void theZoomFloorLetsTheWorldCoverTheView() {
        assertEquals(0.921875F, AtlasView.zoomCovering(236.0F, 512, 128));
    }

    @Test
    void aWorldThatLeavesTheViewIsPulledBackInside() {
        assertEquals(-10.0F, AtlasView.panInside(10.0F, 100.0F, 0.0F, 100.0F));
        assertEquals(50.0F, AtlasView.panInside(-50.0F, 100.0F, 0.0F, 100.0F));
        assertEquals(0.0F, AtlasView.panInside(-10.0F, 120.0F, 0.0F, 100.0F));
    }
}
