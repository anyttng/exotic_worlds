package com.exoticworlds.compat.simpleatlas;

import java.util.List;

import com.exoticworlds.compat.SeamLine;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class AtlasEdgeLines {
    public static void draw(GuiGraphicsExtractor graphics, List<SeamLine> lines, float x, float y, float scale) {
        if (lines.isEmpty()) {
            return;
        }

        int thickness = Math.max(1, Mth.ceil(1.0F / scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        for (SeamLine line : lines) {
            line.fill(graphics, Math.min(thickness, AtlasTileFold.MAP_PIXELS - line.at()));
        }

        graphics.pose().popMatrix();
    }

    private AtlasEdgeLines() {
    }
}
