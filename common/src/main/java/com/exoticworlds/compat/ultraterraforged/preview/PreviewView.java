package com.exoticworlds.compat.ultraterraforged.preview;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.compat.WorldCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record PreviewView(ToroidalShape shape, MapCopies copies, float centerX, float centerZ, float zoom) {
    public static final int PREVIEW_PIXELS = 256;

    public static float zoomCeiling(ToroidalShape shape, MapCopies copies, int widgetPixels) {
        return copies == MapCopies.SINGLE
                ? FullscreenZoomFloor.ultraTerraForgedCoverZoom(shape, PREVIEW_PIXELS)
                : FullscreenZoomFloor.ultraTerraForgedZoom(shape, widgetPixels, PREVIEW_PIXELS);
    }

    public static float heldCenter(ToroidalShape shape, MapCopies copies, Direction.Axis axis, float center,
            float zoom) {
        return copies == MapCopies.SINGLE
                ? (float) AxisCopies.of(shape, axis).clampView(center, PREVIEW_PIXELS / 2.0 * zoom)
                : center;
    }

    public List<SeamLine> seamLines(int widgetX, int widgetY, int widgetPixels) {
        double previewPixelsPerBlock = 1.0 / this.zoom;
        int[] spanX = MapViewSpan.of(this.centerX, PREVIEW_PIXELS, previewPixelsPerBlock, 0);
        int[] spanZ = MapViewSpan.of(this.centerZ, PREVIEW_PIXELS, previewPixelsPerBlock, 0);
        double pixelsPerBlock = widgetPixels * previewPixelsPerBlock / PREVIEW_PIXELS;
        int originX = widgetX + (int) Math.round(widgetPixels / 2.0 - this.centerX * pixelsPerBlock);
        int originY = widgetY + (int) Math.round(widgetPixels / 2.0 - this.centerZ * pixelsPerBlock);
        List<SeamLine> lines = new ArrayList<>();
        for (WorldCopies.Edge edge : WorldCopies.seams(this.shape, spanX[0], spanZ[0], spanX[1], spanZ[1])) {
            lines.add(SeamLine.of(edge, originX, originY, pixelsPerBlock));
        }

        return lines;
    }

    public BlockPos inside(int blockX, int blockZ) {
        return this.shape.fold(new BlockPos(blockX, 0, blockZ));
    }
}
