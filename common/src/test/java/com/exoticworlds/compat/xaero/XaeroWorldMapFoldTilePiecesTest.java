package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;

class XaeroWorldMapFoldTilePiecesTest {
    private static final AxisCopies OFF_GRID = AxisCopies.looped(-937, 1875);

    @Test
    void theTileOnTheSeamSplitsIntoThreeCanonicalPieces() {
        assertEquals(List.of(new TilePiece(234, 0, 2, 0), new TilePiece(-235, 3, 1, 2), new TilePiece(-234, 0, 1, 3)),
                XaeroWorldMapFold.tilePieces(OFF_GRID, 234),
                "chunks 936..937 stay, 938 folds onto -937 (tile -235, slot 3) and 939 onto -936 (tile -234, slot 0)");
    }

    @Test
    void aTilePastTheSeamSitsOneChunkIntoTheCanonicalGrid() {
        assertEquals(List.of(new TilePiece(-234, 1, 3, 0), new TilePiece(-233, 0, 1, 3)),
                XaeroWorldMapFold.tilePieces(OFF_GRID, 235), "chunks 940..943 fold onto -935..-932");
    }

    @Test
    void aTileBeforeTheLowSeamFoldsOntoTheHighEnd() {
        assertEquals(List.of(new TilePiece(232, 3, 1, 0), new TilePiece(233, 0, 3, 1)),
                XaeroWorldMapFold.tilePieces(OFF_GRID, -236), "chunks -944..-941 fold onto 931..934");
    }

    @Test
    void aTileInsideTheWorldIsItsOwnPiece() {
        assertEquals(List.of(new TilePiece(0, 0, 4, 0)), XaeroWorldMapFold.tilePieces(OFF_GRID, 0));
    }

    @Test
    void aWorldOnTheTileGridFoldsWholeTiles() {
        assertEquals(List.of(new TilePiece(-4, 0, 4, 0)), XaeroWorldMapFold.tilePieces(AxisCopies.looped(-16, 32), 4),
                "chunks 16..19 of a 32-chunk world fold onto -16..-13, one whole tile");
    }

    @Test
    void anUnloopedAxisKeepsTheRawTile() {
        assertEquals(List.of(new TilePiece(7, 0, 4, 0)), XaeroWorldMapFold.tilePieces(AxisCopies.UNBOUNDED, 7));
    }
}
