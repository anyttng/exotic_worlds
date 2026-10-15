package com.exoticworlds.compat.simpleatlas;

public record AtlasView(float contentX, float contentY, float contentWidth, float contentHeight) {
    public static float zoomCovering(float viewLength, int worldBlocks, int blocksPerTile, float tilePixelsAtZoomOne) {
        return viewLength * blocksPerTile / (tilePixelsAtZoomOne * worldBlocks);
    }

    public static double panKeepingCentre(double pan, float centre, float originBefore, float tileBefore,
            float origin, float tile) {
        double tilesFromOrigin = (centre - originBefore - pan) / tileBefore;
        return centre - tilesFromOrigin * tile - origin;
    }

    public static float panInside(float worldStart, float worldLength, float viewStart, float viewLength) {
        if (worldLength <= viewLength) {
            return viewStart + (viewLength - worldLength) / 2.0F - worldStart;
        }

        if (worldStart > viewStart) {
            return viewStart - worldStart;
        }

        float worldEnd = worldStart + worldLength;
        float viewEnd = viewStart + viewLength;
        return worldEnd < viewEnd ? viewEnd - worldEnd : 0.0F;
    }
}
