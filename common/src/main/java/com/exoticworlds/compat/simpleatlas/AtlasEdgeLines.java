package com.exoticworlds.compat.simpleatlas;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class AtlasEdgeLines {
    private static final int EDGE_ARGB = 0xCCFFFFFF;

    public static void draw(GuiGraphicsExtractor graphics, List<AtlasTileFold.SeamLine> lines, float x, float y,
            float scale) {
        if (lines.isEmpty()) {
            return;
        }

        int thickness = Math.max(1, Mth.ceil(1.0F / scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        for (AtlasTileFold.SeamLine line : lines) {
            if (line.vertical()) {
                fill(graphics, line.at(), line.from(), line.at() + thickness, line.to());
            } else {
                fill(graphics, line.from(), line.at(), line.to(), line.at() + thickness);
            }
        }

        graphics.pose().popMatrix();
    }

    private static void fill(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        graphics.fill(x0, y0, Math.min(x1, AtlasTileFold.MAP_PIXELS), Math.min(y1, AtlasTileFold.MAP_PIXELS),
                EDGE_ARGB);
    }

    private AtlasEdgeLines() {
    }
}
