package com.exoticworlds.compat.ultimatemapatlases;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class AtlasPointSeat {
    public static Vec3 toward(ResourceKey<Level> dimension, Vec3 anchor, double x, double z) {
        return toward(MapSeamFold.foldFor(null, dimension), anchor, x, z);
    }

    static Vec3 toward(@Nullable WorldFold fold, Vec3 anchor, double x, double z) {
        return NearestCopy.toward(fold, anchor, new Vec3(x, anchor.y, z));
    }

    private AtlasPointSeat() {
    }
}
