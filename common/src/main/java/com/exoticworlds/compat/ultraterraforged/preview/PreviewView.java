package com.exoticworlds.compat.ultraterraforged.preview;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.compat.map.FullscreenZoomFloor;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.map.MapViewSpan;
import com.exoticworlds.compat.map.SeamLine;
import com.exoticworlds.compat.map.WorldCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public record PreviewView(ToroidalShape shape, MapCopies copies, float centerX, float centerZ, float zoom) {
    public static final int PREVIEW_PIXELS = 256;

    private static final float HALF_PREVIEW_PIXELS = PREVIEW_PIXELS / 2.0F;

    private static final double BLOCK_CENTER = 0.5;

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

    public BlockPos blockAt(int widgetX, int widgetY, int widgetPixels, double mouseX, double mouseY) {
        return new BlockPos(this.blockAt(this.centerX, widgetX, widgetPixels, mouseX), 0,
                this.blockAt(this.centerZ, widgetY, widgetPixels, mouseY));
    }

    private int blockAt(float center, int widgetStart, int widgetPixels, double mouse) {
        int pixel = Mth.clamp((int) ((mouse - widgetStart) * PREVIEW_PIXELS / widgetPixels), 0, PREVIEW_PIXELS - 1);
        return Mth.floor(center + (pixel - HALF_PREVIEW_PIXELS) * this.zoom);
    }

    public List<Pixel> copiesOf(BlockPos point, int widgetX, int widgetY, int widgetPixels) {
        double halfBlocks = HALF_PREVIEW_PIXELS * this.zoom;
        AABB window = new AABB(this.centerX - halfBlocks, 0, this.centerZ - halfBlocks,
                this.centerX + halfBlocks, 1, this.centerZ + halfBlocks);
        double pixelsPerBlock = widgetPixels / (PREVIEW_PIXELS * (double) this.zoom);
        List<Pixel> pixels = new ArrayList<>();
        for (ToroidalShape.Oriented<BlockPos> copy : this.shape.copiesInside(window, point)) {
            BlockPos at = copy.value();
            pixels.add(new Pixel(pixelAt(at.getX(), this.centerX, widgetX, widgetPixels, pixelsPerBlock),
                    pixelAt(at.getZ(), this.centerZ, widgetY, widgetPixels, pixelsPerBlock)));
        }

        return pixels;
    }

    private static int pixelAt(int block, float center, int widgetStart, int widgetPixels, double pixelsPerBlock) {
        return widgetStart + (int) Math.floor(widgetPixels / 2.0 + (block + BLOCK_CENTER - center) * pixelsPerBlock);
    }

    public record Pixel(int x, int y) {
    }
}
