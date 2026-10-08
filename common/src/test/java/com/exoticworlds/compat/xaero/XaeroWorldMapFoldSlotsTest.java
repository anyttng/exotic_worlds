package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;

class XaeroWorldMapFoldSlotsTest {
    private static final int SLOT = 64;
    private static final AxisCopies OFF_GRID = AxisCopies.looped(-14992, 30000);

    @Test
    void aSlotAcrossAnOffGridSeamFoldsOntoThreeCanonicalSlots() {
        assertArrayEquals(new int[] {14976, -15040, -14976},
                XaeroWorldMapFold.canonicalSlotOrigins(OFF_GRID, 14976, SLOT),
                "14976..15007 stays, 15008..15039 folds onto -14992..-14961, which crosses the grid line at -14976");
    }

    @Test
    void aSlotPastTheSeamFoldsOntoTheSlotsItsCanonicalSpanTouches() {
        assertArrayEquals(new int[] {-14976, -14912},
                XaeroWorldMapFold.canonicalSlotOrigins(OFF_GRID, 15040, SLOT),
                "15040..15103 folds onto -14960..-14897");
    }

    @Test
    void aSlotInsideTheWorldIsItsOwnCanonicalSlot() {
        assertArrayEquals(new int[] {0}, XaeroWorldMapFold.canonicalSlotOrigins(OFF_GRID, 0, SLOT));
    }

    @Test
    void aSlotAsWideAsAHalfShiftedWorldFoldsOntoBothHalves() {
        assertArrayEquals(new int[] {0, -512},
                XaeroWorldMapFold.canonicalSlotOrigins(AxisCopies.looped(-256, 512), 0, 512),
                "0..255 stays, 256..511 folds onto -256..-1");
    }

    @Test
    void anUnloopedAxisKeepsTheViewSlot() {
        assertArrayEquals(new int[] {15040}, XaeroWorldMapFold.canonicalSlotOrigins(AxisCopies.UNBOUNDED, 15040, SLOT));
    }

    @Test
    void aSlotLeavesTheWorldWhereAnyOfItReachesPastABound() {
        assertTrue(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, 14976, SLOT), "14976..15039 crosses the seam at 15008");
        assertTrue(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, 15040, SLOT), "15040..15103 lies wholly past it");
        assertTrue(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, -15040, SLOT), "-15040..-14977 crosses the seam at -14992");
        assertFalse(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, 14912, SLOT), "14912..14975 lies inside");
        assertFalse(XaeroWorldMapFold.spanLeavesWorld(AxisCopies.UNBOUNDED, 15040, SLOT), "an unbounded axis has no bound");
    }
}
