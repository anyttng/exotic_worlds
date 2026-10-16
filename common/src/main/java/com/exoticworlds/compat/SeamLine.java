package com.exoticworlds.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;

// mc/26.2: Simple Atlas, Seed Viewer and FTB Chunks place and draw their seam lines through it.
public record SeamLine(boolean vertical, int at, int from, int to) {
    public static final int LINE_ARGB = 0xCCFFFFFF;

    public static SeamLine of(WorldCopies.Edge edge, int originX, int originY, double pixelsPerBlock) {
        int fromX = pixel(originX, edge.fromX(), pixelsPerBlock);
        int fromY = pixel(originY, edge.fromZ(), pixelsPerBlock);
        int toX = pixel(originX, edge.toX(), pixelsPerBlock);
        int toY = pixel(originY, edge.toZ(), pixelsPerBlock);
        return edge.fromX() == edge.toX()
                ? new SeamLine(true, fromX, fromY, toY)
                : new SeamLine(false, fromY, fromX, toX);
    }

    public void fill(GuiGraphicsExtractor graphics, int thickness) {
        if (this.vertical) {
            graphics.fill(this.at, this.from, this.at + thickness, this.to, LINE_ARGB);
        } else {
            graphics.fill(this.from, this.at, this.to, this.at + thickness, LINE_ARGB);
        }
    }

    private static int pixel(int originPixel, int block, double pixelsPerBlock) {
        return originPixel + (int) Math.round(block * pixelsPerBlock);
    }
}
