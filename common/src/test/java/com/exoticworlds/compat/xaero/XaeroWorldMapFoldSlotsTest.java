package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.world.level.ChunkPos;

class XaeroWorldMapFoldSlotsTest {
    private static final int SLOT = 64;
    private static final AxisCopies OFF_GRID = AxisCopies.looped(-14992, 30000);
    private static final ToroidalShape OFF_GRID_TORUS = MapShapes.torus(-937, 938);

    @Test
    void aSlotAcrossAnOffGridSeamFoldsOntoThreeCanonicalSlots() {
        assertEquals(row(0, 14976, -15040, -14976),
                origins(XaeroWorldMapFold.canonicalSlotOrigins(OFF_GRID_TORUS, 14976, 0, SLOT)),
                "14976..15007 stays, 15008..15039 folds onto -14992..-14961, which crosses the grid line at -14976");
    }

    @Test
    void aSlotPastTheSeamFoldsOntoTheSlotsItsCanonicalSpanTouches() {
        assertEquals(row(0, -14976, -14912), origins(XaeroWorldMapFold.canonicalSlotOrigins(OFF_GRID_TORUS, 15040, 0, SLOT)),
                "15040..15103 folds onto -14960..-14897");
    }

    @Test
    void aSlotInsideTheWorldIsItsOwnCanonicalSlot() {
        assertEquals(row(0, 0), origins(XaeroWorldMapFold.canonicalSlotOrigins(OFF_GRID_TORUS, 0, 0, SLOT)));
    }

    @Test
    void aSlotAsWideAsAHalfShiftedWorldFoldsOntoBothHalvesOfBothAxes() {
        Set<Long> expected = new HashSet<>(row(0, 0, -512));
        expected.addAll(row(-512, 0, -512));
        assertEquals(expected, origins(XaeroWorldMapFold.canonicalSlotOrigins(MapShapes.torus(-16, 16), 0, 0, 512)),
                "0..255 stays, 256..511 folds onto -256..-1, on X and on Z");
    }

    @Test
    void anUnshapedWorldKeepsTheViewSlot() {
        assertEquals(row(0, 15040), origins(XaeroWorldMapFold.canonicalSlotOrigins(null, 15040, 0, SLOT)));
    }

    @Test
    void aSlotPastALatticeSeamFoldsOntoTheSlotTheSkewMovesItTo() {
        assertEquals(row(-256, -256), origins(XaeroWorldMapFold.canonicalSlotOrigins(
                        MapShapes.latticeTorus(-16, 16, 16), 0, 256, SLOT)),
                "the slot at (0, 256) lies one Z lap up and 256 blocks over, so it folds onto (-256, -256), not (0, -256)");
    }

    @Test
    void aSlotLeavesTheWorldWhereAnyOfItReachesPastABound() {
        assertTrue(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, 14976, SLOT), "14976..15039 crosses the seam at 15008");
        assertTrue(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, 15040, SLOT), "15040..15103 lies wholly past it");
        assertTrue(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, -15040, SLOT), "-15040..-14977 crosses the seam at -14992");
        assertFalse(XaeroWorldMapFold.spanLeavesWorld(OFF_GRID, 14912, SLOT), "14912..14975 lies inside");
        assertFalse(XaeroWorldMapFold.spanLeavesWorld(AxisCopies.UNBOUNDED, 15040, SLOT), "an unbounded axis has no bound");
    }

    private static Set<Long> origins(long[] packed) {
        Set<Long> origins = new HashSet<>();
        for (long origin : packed) {
            origins.add(origin);
        }

        return origins;
    }

    private static Set<Long> row(int z, int... xs) {
        Set<Long> origins = new HashSet<>();
        for (int x : xs) {
            origins.add(ChunkPos.pack(x, z));
        }

        return origins;
    }
}
