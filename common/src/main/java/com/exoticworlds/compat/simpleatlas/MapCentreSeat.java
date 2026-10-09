package com.exoticworlds.compat.simpleatlas;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

public final class MapCentreSeat {
    public static int toward(@Nullable WorldFold fold, Direction.Axis axis, double x, double z, MapItemSavedData data) {
        return (int) NearestCopy.toward(fold, axis, new Vec3(x, 0.0, z), new Vec3(data.centerX, 0.0, data.centerZ));
    }

    private MapCentreSeat() {
    }
}
