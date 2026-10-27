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
    public static int toward(@Nullable WorldFold fold, Direction.Axis axis, double x, double z, MapItemSavedData data) {
        return (int) NearestCopy.toward(fold, axis, new Vec3(x, 0.0, z), new Vec3(data.centerX, 0.0, data.centerZ));
    }

    public static int towardWaypoint(Direction.Axis axis, AtlasContents.WaypointData waypoint, MapItemSavedData data) {
        return toward(MapSeamFold.foldFor(null, data.dimension), axis, waypoint.worldX(), waypoint.worldZ(),
                data);
    }

    private MapCentreSeat() {
    }
}
