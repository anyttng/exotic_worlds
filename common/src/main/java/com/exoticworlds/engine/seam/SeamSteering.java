package com.exoticworlds.engine.seam;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamSteering {
    public static Vec3 nearestCopy(Entity body, Vec3 target) {
        WorldFold fold = ((FoldSource) body).exotic_worlds$wrappedFold();
        return NearestCopy.toward(fold, body.position(), target);
    }

    public static BlockPos nearestCopy(Entity body, BlockPos target) {
        WorldFold fold = ((FoldSource) body).exotic_worlds$wrappedFold();
        return NearestCopy.toward(fold, body.blockPosition(), target);
    }

    private SeamSteering() {
    }
}
