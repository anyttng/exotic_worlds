package com.exoticworlds.compat;

public final class MapViewSpan {
    public static int[] of(double center, int windowPixels, double pixelsPerBlock, int margin) {
        double halfSpan = halfBlocks(windowPixels, pixelsPerBlock);
        return new int[] {(int) Math.floor(center - halfSpan) - margin, (int) Math.ceil(center + halfSpan) + margin};
    }

    public static double halfBlocks(int windowPixels, double pixelsPerBlock) {
        return windowPixels / 2.0 / pixelsPerBlock;
    }

    private MapViewSpan() {
    }
}
