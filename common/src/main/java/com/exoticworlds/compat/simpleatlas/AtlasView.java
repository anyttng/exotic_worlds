package com.exoticworlds.compat.simpleatlas;

public record AtlasView(float contentX, float contentY, float contentWidth, float contentHeight) {
    public static final float MIN_ZOOM = 0.25F;

    public static final float MAX_ZOOM = 4.0F;

    public static final float TILE_PIXELS_AT_ZOOM_ONE = 64.0F;

    private static final float SCREEN_MARGIN = 32.0F;

    private static final float PANEL_WIDTH = 256.0F;

    private static final float PANEL_HEIGHT = 180.0F;

    private static final float MIN_PANEL_SCALE = 0.25F;

    private static final float MAX_PANEL_SCALE = 3.0F;

    private static final float CONTENT_LEFT = 10.0F;

    private static final float CONTENT_TOP = 18.0F;

    private static final float CONTENT_WIDTH = 236.0F;

    private static final float CONTENT_HEIGHT = 143.0F;

    // Simple Atlas keeps its viewport type private, so AtlasScreen.getAtlasViewport's layout is restated here.
    public static AtlasView of(int screenWidth, int screenHeight) {
        float availableWidth = Math.max(SCREEN_MARGIN, screenWidth - SCREEN_MARGIN);
        float availableHeight = Math.max(SCREEN_MARGIN, screenHeight - SCREEN_MARGIN);
        float scale = Math.clamp(availableWidth / PANEL_WIDTH, MIN_PANEL_SCALE,
                Math.min(MAX_PANEL_SCALE, availableHeight / PANEL_HEIGHT));
        float x = (screenWidth - PANEL_WIDTH * scale) / 2.0F;
        float y = (screenHeight - PANEL_HEIGHT * scale) / 2.0F;
        return new AtlasView(x + CONTENT_LEFT * scale, y + CONTENT_TOP * scale, CONTENT_WIDTH * scale,
                CONTENT_HEIGHT * scale);
    }

    public static float zoomCovering(float viewLength, int worldBlocks, int blocksPerTile) {
        return viewLength * blocksPerTile / (TILE_PIXELS_AT_ZOOM_ONE * worldBlocks);
    }

    public static float panInside(float worldStart, float worldLength, float viewStart, float viewLength) {
        if (worldStart > viewStart) {
            return viewStart - worldStart;
        }

        float worldEnd = worldStart + worldLength;
        float viewEnd = viewStart + viewLength;
        return worldEnd < viewEnd ? viewEnd - worldEnd : 0.0F;
    }
}
