package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.compat.WorldCopies;
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

    public static int screenShortSide() {
        Window window = Minecraft.getInstance().getWindow();
        return Math.min(window.getWidth(), window.getHeight());
    }

    public double beginFrame(double scaleMultiplier) {
        this.mapCopies = MapCopies.current();
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
        return clampCamera(XaeroWorldMapFold.browsedShape(), this.mapCopies, Direction.Axis.X, cameraX,
                Minecraft.getInstance().getWindow().getWidth(), scale);
    }

    public double clampCameraZ(double cameraZ, double scale) {
        return clampCamera(XaeroWorldMapFold.browsedShape(), this.mapCopies, Direction.Axis.Z, cameraZ,
                Minecraft.getInstance().getWindow().getHeight(), scale);
    }

    static double clampCamera(@Nullable ToroidalShape shape, MapCopies mapCopies, Direction.Axis axis, double camera,
            int windowPixels, double scale) {
        if (mapCopies != MapCopies.SINGLE || shape == null) {
            return camera;
        }

        return AxisCopies.of(shape, axis).clampView(camera, MapViewSpan.halfBlocks(windowPixels, scale));
    }

    public void drawSeamGrid(Matrix4f matrix, VertexConsumer buffer, int flooredCameraX, int flooredCameraZ,
            double cameraX, double cameraZ, double scale) {
        Window window = Minecraft.getInstance().getWindow();
        for (SeamFill fill : seamFills(XaeroWorldMapFold.browsedShape(), this.mapCopies.drawsSeamLines(), cameraX,
                cameraZ, window.getWidth(), window.getHeight(), scale)) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, buffer, fill.minX() - flooredCameraX,
                    fill.minZ() - flooredCameraZ, fill.maxX() - flooredCameraX, fill.maxZ() - flooredCameraZ,
                    ARGB.redFloat(SeamLine.LINE_ARGB), ARGB.greenFloat(SeamLine.LINE_ARGB),
                    ARGB.blueFloat(SeamLine.LINE_ARGB), ARGB.alphaFloat(SeamLine.LINE_ARGB));
        }
    }

    static List<SeamFill> seamFills(@Nullable ToroidalShape shape, boolean drawsSeamLines, double cameraX,
            double cameraZ, int windowWidth, int windowHeight, double scale) {
        if (!drawsSeamLines || shape == null) {
            return List.of();
        }

        int thickness = Math.max(1, (int) Math.ceil(1.0 / scale));
        int[] spanX = MapViewSpan.of(cameraX, windowWidth, scale, thickness);
        int[] spanZ = MapViewSpan.of(cameraZ, windowHeight, scale, thickness);
        List<SeamFill> fills = new ArrayList<>();
        for (WorldCopies.Edge seam : WorldCopies.seams(shape, spanX[0], spanZ[0], spanX[1], spanZ[1])) {
            fills.add(new SeamFill(seam.fromX(), seam.fromZ(), Math.max(seam.toX(), seam.fromX() + thickness),
                    Math.max(seam.toZ(), seam.fromZ() + thickness)));
        }

        return fills;
    }
}
