package com.exoticworlds.compat.xaero;

import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePiece;
import com.exoticworlds.core.world.CoordinateConstants;

public record XaeroPieceRect(float x, float y, int textureX, int textureY, float width, float height) {
    static XaeroPieceRect of(XaeroPieceRect tile, TilePiece pieceX, TilePiece pieceZ) {
        int textureY = isStartingOnTileRow(pieceZ)
                ? tile.textureY() : (pieceZ.firstInside() + pieceZ.count()) * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
        return new XaeroPieceRect(tile.x() + pieceX.rawOffset() * CoordinateConstants.CHUNK_WIDTH_BLOCKS,
                tile.y() + pieceZ.rawOffset() * CoordinateConstants.CHUNK_WIDTH_BLOCKS,
                pieceX.firstInside() * CoordinateConstants.CHUNK_WIDTH_BLOCKS, textureY,
                pieceX.count() * CoordinateConstants.CHUNK_WIDTH_BLOCKS, pieceZ.count() * CoordinateConstants.CHUNK_WIDTH_BLOCKS);
    }

    static boolean isStartingOnTileRow(TilePiece pieceZ) {
        return pieceZ.firstInside() == 0;
    }
}
