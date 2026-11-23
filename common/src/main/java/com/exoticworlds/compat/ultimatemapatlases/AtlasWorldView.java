package com.exoticworlds.compat.ultimatemapatlases;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.FullscreenZoomFloor;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class AtlasWorldView {
    private static final float FIRST_DISCRETE_ZOOM = 2.0F;

    public static float zoomCeiling(@Nullable WorldFold worldFold, MapCopies copies, int width, int height,
            int mapBlocks) {
        if (worldFold == null) {
            return Float.MAX_VALUE;
        }

        return copies == MapCopies.SINGLE
                ? FullscreenZoomFloor.ultimateMapAtlasesCoverZoom(worldFold, width, height, mapBlocks)
                : FullscreenZoomFloor.ultimateMapAtlasesZoom(worldFold, width, mapBlocks);
    }

    public static float discreteZoomSteps(float ceiling) {
        return Math.max(0.0F, 2.0F * (float) Math.floor((ceiling - FIRST_DISCRETE_ZOOM) / 2.0F));
    }

    public static Vec3 held(@Nullable WorldFold worldFold, Vec3 center, float zoom, int width, int height,
            int mapBlocks, boolean fold) {
        Vec3 seated = fold ? ClientFolds.fold(worldFold, center) : center;
        double halfX = zoom * mapBlocks / 2.0;
        double halfZ = halfX * height / width;
        return new Vec3(AxisCopies.of(worldFold, Direction.Axis.X).clampView(seated.x, halfX), 0.0,
                AxisCopies.of(worldFold, Direction.Axis.Z).clampView(seated.z, halfZ));
    }

    private AtlasWorldView() {
    }
}
