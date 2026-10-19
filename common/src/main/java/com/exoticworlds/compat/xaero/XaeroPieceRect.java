package com.exoticworlds.compat.xaero;

import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;
import com.exoticworlds.core.CoordinateConstants;

public record XaeroPieceRect(float x, float y, int textureX, int textureY, float width, float height) {
    static XaeroPieceRect of(XaeroPieceRect tile, TilePiece pieceX, TilePiece pieceZ) {
        int textureY = startsOnTileRow(pieceZ)
                ? tile.textureY() : (pieceZ.firstInside() + pieceZ.count()) * CoordinateConstants.CHUNK_WIDTH;
        return new XaeroPieceRect(tile.x() + pieceX.rawOffset() * CoordinateConstants.CHUNK_WIDTH,
                tile.y() + pieceZ.rawOffset() * CoordinateConstants.CHUNK_WIDTH,
                pieceX.firstInside() * CoordinateConstants.CHUNK_WIDTH, textureY,
                pieceX.count() * CoordinateConstants.CHUNK_WIDTH, pieceZ.count() * CoordinateConstants.CHUNK_WIDTH);
    }

    static boolean startsOnTileRow(TilePiece pieceZ) {
        return pieceZ.firstInside() == 0;
    }
}
