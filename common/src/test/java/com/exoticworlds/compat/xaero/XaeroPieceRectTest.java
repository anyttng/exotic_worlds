package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TileArea;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;

class XaeroPieceRectTest {
    private static final XaeroPieceRect TILE = new XaeroPieceRect(10.0F, 20.0F, 0, 7, 64.0F, 64.0F);
    private static final TilePiece WHOLE = new TilePiece(0, 0, 4, 0);

    @Test
    void aPieceOnTheTilesFirstRowKeepsXaerosRowAndMovesByItsRawOffset() {
        XaeroPieceRect rect = XaeroPieceRect.of(TILE, new TilePiece(-235, 3, 1, 2), WHOLE);
        assertEquals(new XaeroPieceRect(42.0F, 20.0F, 48, 7, 16.0F, 64.0F), rect,
                "the third raw chunk of the tile shows canonical slot 3, one chunk wide");
        assertTrue(XaeroPieceRect.startsOnTileRow(WHOLE));
    }

    @Test
    void aPieceStartingInsideTheTileOnZReadsUpFromItsLastRow() {
        TilePiece insideZ = new TilePiece(-234, 1, 3, 0);
        XaeroPieceRect rect = XaeroPieceRect.of(TILE, WHOLE, insideZ);
        assertEquals(new XaeroPieceRect(10.0F, 20.0F, 0, 64, 64.0F, 48.0F), rect,
                "slots 1..3 of the canonical tile, drawn upward from the end of slot 3");
        assertFalse(XaeroPieceRect.startsOnTileRow(insideZ));
    }

    @Test
    void theSeamTilesPiecesCoverTheTileOnce() {
        WorldCopies.Window window = MapShapes.windows(MapShapes.torus(-937, 938)).getLast();
        float covered = 0.0F;
        for (TileArea area : XaeroWorldMapFold.tilePieces(window, 234, 0)) {
            covered += XaeroPieceRect.of(TILE, area.x(), area.z()).width();
        }

        assertEquals(TILE.width(), covered, "chunks 936..939 split three ways and fill the tile's 64 pixels");
    }
}
