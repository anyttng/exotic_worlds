package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class XaeroCursorLapTest {
    private static final ToroidalShape SMALL = MapShapes.torus(-16, 16);
    private static final int LAP = 512;

    @Test
    void aCursorPastTheSeamFoldsAndKeepsItsLap() {
        XaeroCursorLap cursor = new XaeroCursorLap();
        assertEquals(new BlockPos(-212, 0, 40), cursor.foldCursor(SMALL, 300, 40), "300 folds onto 300 - 512");
        assertEquals(LAP, cursor.seatX(0), "the highlight is drawn back on the cursor's lap");
        assertEquals(7, cursor.seatZ(7));
    }

    @Test
    void anUnshapedWorldKeepsTheCursorWhereItIs() {
        XaeroCursorLap cursor = new XaeroCursorLap();
        assertEquals(new BlockPos(300, 0, 40), cursor.foldCursor(null, 300, 40));
        assertEquals(0, cursor.seatX(0));
    }

    @Test
    void aFreshSelectionSeatsItsEndNearestItsStart() {
        XaeroCursorLap cursor = new XaeroCursorLap();
        BlockPos folded = cursor.foldCursor(SMALL, 270, 0);
        assertEquals(new ChunkPos(16, 0), cursor.selectionEnd(SMALL, new Object(), 15, 0, folded.getX(), folded.getZ()),
                "the cursor at 270 folds into chunk -16, whose copy nearest chunk 15 is 16");
        assertEquals(15, cursor.seatSelectionX(15), "the unwrapped end already carries the lap, so the box stays put");
    }

    @Test
    void theSecondArgumentReadsTheEndTheFirstSeated() {
        XaeroCursorLap cursor = new XaeroCursorLap();
        Object selection = new Object();
        BlockPos folded = cursor.foldCursor(SMALL, 270, 0);
        ChunkPos first = cursor.selectionEnd(SMALL, selection, 15, 0, folded.getX(), folded.getZ());
        assertEquals(first, cursor.selectionEnd(SMALL, selection, 0, 0, 0, 0));
    }

    @Test
    void aDragContinuesFromThePreviousEnd() {
        XaeroCursorLap cursor = new XaeroCursorLap();
        Object selection = new Object();
        BlockPos folded = cursor.foldCursor(SMALL, 270, 0);
        cursor.selectionEnd(SMALL, selection, 15, 0, folded.getX(), folded.getZ());
        cursor.selectionEnd(SMALL, selection, 15, 0, folded.getX(), folded.getZ());
        folded = cursor.foldCursor(SMALL, 290, 0);
        assertEquals(new ChunkPos(18, 0), cursor.selectionEnd(SMALL, selection, 15, 0, folded.getX(), folded.getZ()),
                "chunk -14's copy nearest the previous end 16 is 18");
    }

    @Test
    void theEndStaysWithinOneLapOfTheStart() {
        XaeroCursorLap cursor = new XaeroCursorLap();
        Object selection = new Object();
        int rawX = 240;
        ChunkPos end = ChunkPos.ZERO;
        for (int step = 0; step < 40; step++) {
            BlockPos folded = cursor.foldCursor(SMALL, rawX, 0);
            end = cursor.selectionEnd(SMALL, selection, 15, 0, folded.getX(), folded.getZ());
            cursor.selectionEnd(SMALL, selection, 15, 0, folded.getX(), folded.getZ());
            rawX += 16;
        }

        assertEquals(new ChunkPos(46, 0), end, "a drag of more than a lap holds the end 31 chunks past the start");
    }

    @Test
    void anUnshapedWorldKeepsTheRawEnd() {
        XaeroCursorLap cursor = new XaeroCursorLap();
        BlockPos folded = cursor.foldCursor(null, 270, 0);
        assertEquals(new ChunkPos(16, 0), cursor.selectionEnd(null, new Object(), 15, 0, folded.getX(), folded.getZ()));
        assertEquals(15, cursor.seatSelectionX(15));
    }
}
