package com.exoticworlds.compat.map;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public record SeamLine(boolean isVertical, int at, int from, int to) {
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

    public void fill(GuiGraphicsExtractor graphics, int thicknessPixels) {
        if (this.isVertical) {
            graphics.fill(this.at, this.from, this.at + thicknessPixels, this.to, LINE_ARGB);
        } else {
            graphics.fill(this.from, this.at, this.to, this.at + thicknessPixels, LINE_ARGB);
        }
    }

    private static int pixel(int originPixel, int block, double pixelsPerBlock) {
        return originPixel + (int) Math.round(block * pixelsPerBlock);
    }
}
