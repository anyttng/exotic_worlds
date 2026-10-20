package com.exoticworlds.compat.ultimatemapatlases;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

public final class AtlasCenterSeat {
    public static int toward(Direction.Axis axis, MapItemSavedData data, double x, double z) {
        return toward(MapSeamFold.transformerFor(null, data.dimension), axis, data.centerX, data.centerZ, x, z);
    }

    static int toward(@Nullable WorldFold fold, Direction.Axis axis, int centerX, int centerZ, double x, double z) {
        return (int) NearestCopy.toward(fold, axis, new Vec3(x, 0.0, z), new Vec3(centerX, 0.0, centerZ));
    }

    private AtlasCenterSeat() {
    }
}
