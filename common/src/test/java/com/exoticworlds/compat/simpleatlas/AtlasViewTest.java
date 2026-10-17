package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;

class AtlasViewTest {
    private static final float ONE_PIXEL_PER_BLOCK = 1.0F;

    @Test
    void aZoomChangeKeepsTheMapUnderTheViewCentre() {
        assertEquals(-40.0, AtlasView.panKeepingCentre(10.0, 100.0F, 20.0F, 64.0F, 0.0F, 128.0F));
    }

    @Test
    void aWorldThatLeavesTheViewIsPulledBackInside() {
        assertEquals(-10.0F, AtlasView.panInside(AxisCopies.looped(0, 100), 10.0F, 0.0F, 100.0F, ONE_PIXEL_PER_BLOCK));
        assertEquals(50.0F, AtlasView.panInside(AxisCopies.looped(0, 100), -50.0F, 0.0F, 100.0F, ONE_PIXEL_PER_BLOCK));
        assertEquals(0.0F, AtlasView.panInside(AxisCopies.looped(0, 120), -10.0F, 0.0F, 100.0F, ONE_PIXEL_PER_BLOCK));
    }

    @Test
    void aWorldPulledInsideStaysWhereItWasPut() {
        for (int worldBlocks : new int[] {60, 100, 140}) {
            AxisCopies axis = AxisCopies.looped(0, worldBlocks);
            float worldStart = 30.0F;
            worldStart += AtlasView.panInside(axis, worldStart, 0.0F, 100.0F, ONE_PIXEL_PER_BLOCK);
            assertEquals(0.0F, AtlasView.panInside(axis, worldStart, 0.0F, 100.0F, ONE_PIXEL_PER_BLOCK),
                    "world " + worldBlocks);
        }
    }
}
