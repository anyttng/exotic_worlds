package com.exoticworlds.compat.seedviewer;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.core.ToroidalShapeView;
import com.exoticworlds.core.WorldFold;

import net.acenia.seedviewer.client.map.MapCamera;
import net.acenia.seedviewer.client.map.MapViewport;
import net.acenia.seedviewer.client.worldgen.BiomeSampler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

public final class SeamLines {
    private static final int SEAM_ARGB = 0xCCFFFFFF;

    public static void draw(GuiGraphicsExtractor graphics, BiomeSampler sampler, MapCamera camera,
            MapViewport viewport) {
        WorldFold fold = ((FoldedBiomeSampler) (Object) sampler).toroidal$fold();
        if (!fold.isWrapped()) {
            return;
        }

        ToroidalShape shape = new ToroidalShapeView(fold);
        if (narrowerThanFloor(AxisCopies.of(shape, Direction.Axis.X), camera)
                || narrowerThanFloor(AxisCopies.of(shape, Direction.Axis.Z), camera)) {
            return;
        }

        int minX = (int) Math.floor(camera.screenToWorldX(viewport.x(), viewport.x(), viewport.width()));
        int maxX = (int) Math.ceil(camera.screenToWorldX(viewport.right(), viewport.x(), viewport.width()));
        int minZ = (int) Math.floor(camera.screenToWorldZ(viewport.y(), viewport.y(), viewport.height()));
        int maxZ = (int) Math.ceil(camera.screenToWorldZ(viewport.bottom(), viewport.y(), viewport.height()));
        for (WorldCopies.Edge seam : WorldCopies.seams(shape, minX, minZ, maxX, maxZ)) {
            int x0 = screenX(camera, viewport, seam.fromX());
            int y0 = screenY(camera, viewport, seam.fromZ());
            int x1 = screenX(camera, viewport, seam.toX());
            int y1 = screenY(camera, viewport, seam.toZ());
            graphics.fill(x0, y0, Math.max(x1, x0 + 1), Math.max(y1, y0 + 1), SEAM_ARGB);
        }
    }

    private static boolean narrowerThanFloor(AxisCopies axis, MapCamera camera) {
        return axis.loops() && axis.width() * camera.pixelsPerBlock() < FullscreenZoomFloor.MIN_WORLD_PIXELS;
    }

    private static int screenX(MapCamera camera, MapViewport viewport, int blockX) {
        return (int) Math.floor(camera.worldToScreenX(blockX, viewport.x(), viewport.width()));
    }

    private static int screenY(MapCamera camera, MapViewport viewport, int blockZ) {
        return (int) Math.floor(camera.worldToScreenY(blockZ, viewport.y(), viewport.height()));
    }

    private SeamLines() {
    }
}
