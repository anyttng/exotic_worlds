package com.exoticworlds.compat.map;

public final class MapViewSpan {
    public static int[] of(double center, int windowPixels, double pixelsPerBlock, int marginBlocks) {
        double halfSpanBlocks = halfBlocks(windowPixels, pixelsPerBlock);
        return new int[] {(int) Math.floor(center - halfSpanBlocks) - marginBlocks, (int) Math.ceil(center + halfSpanBlocks) + marginBlocks};
    }

    public static double halfBlocks(int windowPixels, double pixelsPerBlock) {
        return windowPixels / 2.0 / pixelsPerBlock;
    }

    private MapViewSpan() {
    }
}
