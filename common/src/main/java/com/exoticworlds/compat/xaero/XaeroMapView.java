package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.map.MapViewSpan;
import com.exoticworlds.compat.map.SeamLine;
import com.exoticworlds.compat.map.WorldCopies;
import com.exoticworlds.core.fold.WorldFold;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;

import xaero.map.graphics.MapRenderHelper;

public final class XaeroMapView {
    private MapCopies mapCopies = MapCopies.REPEATED;

    public record SeamFill(int minX, int minZ, int maxX, int maxZ) {
    }

    public static int getScreenShortSidePixels() {
        Window window = Minecraft.getInstance().getWindow();
        return Math.min(window.getWidth(), window.getHeight());
    }

    public double beginFrame(double scaleMultiplier) {
        this.mapCopies = MapCopies.getCurrent();
        return zoomFloor(scaleMultiplier);
    }

    public double zoomFloor(double scaleMultiplier) {
        Window window = Minecraft.getInstance().getWindow();
        return XaeroWorldMapFold.zoomFloorScale(scaleMultiplier, this.mapCopies, window.getWidth(), window.getHeight());
    }

    public static double raised(double scale, double floor) {
        return floor > 0.0 && scale < floor ? floor : scale;
    }

    public double clampCameraX(double cameraX, double scale) {
        return clampCamera(XaeroWorldMapFold.getBrowsedFold(), this.mapCopies, Direction.Axis.X, cameraX,
                MapViewSpan.halfBlocks(Minecraft.getInstance().getWindow().getWidth(), scale));
    }

    public double clampCameraZ(double cameraZ, double scale) {
        return clampCamera(XaeroWorldMapFold.getBrowsedFold(), this.mapCopies, Direction.Axis.Z, cameraZ,
                MapViewSpan.halfBlocks(Minecraft.getInstance().getWindow().getHeight(), scale));
    }

    static double clampCamera(@Nullable WorldFold fold, MapCopies mapCopies, Direction.Axis axis, double camera,
            double halfSpanBlocks) {
        if (mapCopies != MapCopies.SINGLE || fold == null) {
            return camera;
        }

        return AxisCopies.of(fold, axis).clampView(camera, halfSpanBlocks);
    }

    public void drawSeamGrid(Matrix4f matrix, VertexConsumer buffer, int flooredCameraX, int flooredCameraZ,
            XaeroCamera camera) {
        Window window = Minecraft.getInstance().getWindow();
        for (SeamFill fill : seamFills(XaeroWorldMapFold.getBrowsedFold(), this.mapCopies.isDrawingSeamLines(), camera,
                window.getWidth(), window.getHeight())) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, buffer, fill.minX() - flooredCameraX,
                    fill.minZ() - flooredCameraZ, fill.maxX() - flooredCameraX, fill.maxZ() - flooredCameraZ,
                    ARGB.redFloat(SeamLine.LINE_ARGB), ARGB.greenFloat(SeamLine.LINE_ARGB),
                    ARGB.blueFloat(SeamLine.LINE_ARGB), ARGB.alphaFloat(SeamLine.LINE_ARGB));
        }
    }

    static List<SeamFill> seamFills(@Nullable WorldFold fold, boolean isDrawingSeamLines, XaeroCamera camera,
            int windowWidthPixels, int windowHeightPixels) {
        if (!isDrawingSeamLines || fold == null) {
            return List.of();
        }

        double scale = camera.scale();
        int thicknessPixels = Math.max(1, (int) Math.ceil(1.0 / scale));
        int[] spanX = MapViewSpan.of(camera.x(), windowWidthPixels, scale, thicknessPixels);
        int[] spanZ = MapViewSpan.of(camera.z(), windowHeightPixels, scale, thicknessPixels);
        List<SeamFill> fills = new ArrayList<>();
        for (WorldCopies.Edge seam : WorldCopies.seams(fold, spanX[0], spanZ[0], spanX[1], spanZ[1])) {
            fills.add(new SeamFill(seam.fromX(), seam.fromZ(), Math.max(seam.toX(), seam.fromX() + thicknessPixels),
                    Math.max(seam.toZ(), seam.fromZ() + thicknessPixels)));
        }

        return fills;
    }
}
