package com.exoticworlds.compat.ultimatemapatlases;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.compat.WorldCopies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class AtlasSeamLines {
    public static List<SeamLine> of(@Nullable ToroidalShape shape, int tileCenterX, int tileCenterZ, int mapBlocks) {
        int blocksPerPixel = mapBlocks / AtlasTileCopies.MAP_PIXELS;
        int minX = tileCenterX - mapBlocks / 2;
        int minZ = tileCenterZ - mapBlocks / 2;
        List<SeamLine> lines = new ArrayList<>();
        for (WorldCopies.Edge edge : WorldCopies.seams(shape, minX, minZ, minX + mapBlocks, minZ + mapBlocks)) {
            lines.add(SeamLine.of(edge, -minX / blocksPerPixel, -minZ / blocksPerPixel, 1.0 / blocksPerPixel));
        }

        return lines;
    }

    public static void draw(GuiGraphicsExtractor graphics, List<SeamLine> lines) {
        if (lines.isEmpty()) {
            return;
        }

        int thickness = Math.max(1, Mth.ceil(1.0F / graphics.pose().m00()));
        graphics.nextStratum();
        for (SeamLine line : lines) {
            line.fill(graphics, Math.min(thickness, AtlasTileCopies.MAP_PIXELS - line.at()));
        }
    }

    private AtlasSeamLines() {
    }
}
