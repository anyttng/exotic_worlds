package com.exoticworlds.engine.seam;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.SeamDelta;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamAim {
    public static double nearestCoord(Entity viewer, Entity target, Direction.Axis axis, double coord) {
        return SeamSteering.nearestCopy(viewer, target.position().with(axis, coord)).get(axis);
    }

    public static Vec3 deltaTo(Entity viewer, Vec3 point) {
        return WorldLoopAttachments.transformerOf(viewer.level()).foldDelta(viewer.position(), point);
    }

    public static Vec3 foldDelta(Entity levelSource, double deltaX, double deltaZ) {
        return SeamDelta.fold(WorldLoopAttachments.transformerOf(levelSource.level()), deltaX, deltaZ);
    }

    public static Vec3 foldDelta(Entity levelSource, Vec3 delta) {
        return SeamDelta.fold(WorldLoopAttachments.transformerOf(levelSource.level()), delta);
    }

    private SeamAim() {
    }
}
