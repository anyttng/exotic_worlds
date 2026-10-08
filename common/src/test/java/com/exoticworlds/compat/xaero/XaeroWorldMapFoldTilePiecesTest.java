package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TileArea;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;

class XaeroWorldMapFoldTilePiecesTest {
    private static final ToroidalShape OFF_GRID = MapShapes.torus(-937, 938);
    private static final TilePiece WHOLE_ZERO = new TilePiece(0, 0, 4, 0);

    @Test
    void theTileOnTheSeamSplitsIntoThreeCanonicalPieces() {
        assertEquals(alongX(new TilePiece(234, 0, 2, 0), new TilePiece(-235, 3, 1, 2), new TilePiece(-234, 0, 1, 3)),
                XaeroWorldMapFold.tilePieces(OFF_GRID, 234, 0),
                "chunks 936..937 stay, 938 folds onto -937 (tile -235, slot 3) and 939 onto -936 (tile -234, slot 0)");
    }

    @Test
    void aTilePastTheSeamSitsOneChunkIntoTheCanonicalGrid() {
        assertEquals(alongX(new TilePiece(-234, 1, 3, 0), new TilePiece(-233, 0, 1, 3)),
                XaeroWorldMapFold.tilePieces(OFF_GRID, 235, 0), "chunks 940..943 fold onto -935..-932");
    }

    @Test
    void aTileBeforeTheLowSeamFoldsOntoTheHighEnd() {
        assertEquals(alongX(new TilePiece(232, 3, 1, 0), new TilePiece(233, 0, 3, 1)),
                XaeroWorldMapFold.tilePieces(OFF_GRID, -236, 0), "chunks -944..-941 fold onto 931..934");
    }

    @Test
    void aTileInsideTheWorldIsItsOwnPiece() {
        assertEquals(alongX(WHOLE_ZERO), XaeroWorldMapFold.tilePieces(OFF_GRID, 0, 0));
    }

    @Test
    void aWorldOnTheTileGridFoldsWholeTiles() {
        assertEquals(alongX(new TilePiece(-4, 0, 4, 0)), XaeroWorldMapFold.tilePieces(MapShapes.torus(-16, 16), 4, 0),
                "chunks 16..19 of a 32-chunk world fold onto -16..-13, one whole tile");
    }

    @Test
    void aTilePastALatticeSeamFoldsOntoTheTileTheSkewMovesItTo() {
        assertEquals(List.of(new TileArea(new TilePiece(-2, 0, 4, 0), new TilePiece(-4, 0, 4, 0))),
                XaeroWorldMapFold.tilePieces(MapShapes.latticeTorus(-16, 16, 8), 0, 4),
                "chunks (0..3, 16..19) lie one Z lap up and 8 chunks over, so they fold onto (-8..-5, -16..-13)");
    }

    @Test
    void anUnshapedWorldKeepsTheRawTile() {
        assertEquals(List.of(new TileArea(new TilePiece(7, 0, 4, 0), new TilePiece(-3, 0, 4, 0))),
                XaeroWorldMapFold.tilePieces(null, 7, -3));
    }

    private static List<TileArea> alongX(TilePiece... piecesX) {
        return Arrays.stream(piecesX).map(pieceX -> new TileArea(pieceX, WHOLE_ZERO)).toList();
    }
}
