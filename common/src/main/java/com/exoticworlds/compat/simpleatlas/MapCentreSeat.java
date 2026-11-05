package com.exoticworlds.compat.simpleatlas;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

import rubbertoe.simple_atlas.component.AtlasContents;

public final class MapCentreSeat {
    public static int toward(@Nullable WorldFold fold, Direction.Axis axis, int center, double x, double z,
            MapItemSavedData data) {
        return toward(fold, axis, centerX(axis, center, data), centerZ(axis, center, data), x, z);
    }

    public static int towardWaypoint(Direction.Axis axis, int center, AtlasContents.WaypointData waypoint,
            MapItemSavedData data) {
        return toward(MapSeamFold.foldFor(null, data.dimension), axis, center, waypoint.worldX(), waypoint.worldZ(),
                data);
    }

    static int toward(@Nullable WorldFold fold, Direction.Axis axis, int centerX, int centerZ, double x, double z) {
        return (int) NearestCopy.toward(fold, axis, new Vec3(x, 0.0, z), new Vec3(centerX, 0.0, centerZ));
    }

    private static int centerX(Direction.Axis axis, int center, MapItemSavedData data) {
        return axis == Direction.Axis.X ? center : data.centerX;
    }

    private static int centerZ(Direction.Axis axis, int center, MapItemSavedData data) {
        return axis == Direction.Axis.Z ? center : data.centerZ;
    }

    private MapCentreSeat() {
    }
}
