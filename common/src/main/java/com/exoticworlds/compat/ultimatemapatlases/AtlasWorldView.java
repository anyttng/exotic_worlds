package com.exoticworlds.compat.ultimatemapatlases;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class AtlasWorldView {
    private static final float FIRST_DISCRETE_ZOOM = 2.0F;

    public static float zoomCeiling(@Nullable ToroidalShape shape, MapCopies copies, int width, int height,
            int mapBlocks) {
        if (shape == null) {
            return Float.MAX_VALUE;
        }

        return copies == MapCopies.SINGLE
                ? FullscreenZoomFloor.ultimateMapAtlasesCoverZoom(shape, width, height, mapBlocks)
                : FullscreenZoomFloor.ultimateMapAtlasesZoom(shape, width, mapBlocks);
    }

    public static float discreteZoomSteps(float ceiling) {
        return Math.max(0.0F, 2.0F * (float) Math.floor((ceiling - FIRST_DISCRETE_ZOOM) / 2.0F));
    }

    public static Vec3 held(@Nullable ToroidalShape shape, Vec3 center, float zoom, int width, int height,
            int mapBlocks, boolean fold) {
        Vec3 seated = fold ? ClientShapes.fold(shape, center) : center;
        double halfX = zoom * mapBlocks / 2.0;
        double halfZ = halfX * height / width;
        return new Vec3(AxisCopies.of(shape, Direction.Axis.X).clampView(seated.x, halfX), 0.0,
                AxisCopies.of(shape, Direction.Axis.Z).clampView(seated.z, halfZ));
    }

    private AtlasWorldView() {
    }
}
