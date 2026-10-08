package com.exoticworlds.engine.fold;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class SeamDelta {
    public static Vec3 fold(WorldFold fold, Vec3 delta) {
        return fold.nearestCopy(Vec3.ZERO, delta);
    }

    public static Vec3 fold(WorldFold fold, double deltaX, double deltaZ) {
        return fold(fold, new Vec3(deltaX, 0.0, deltaZ));
    }

    public static BlockPos fold(WorldFold fold, BlockPos delta) {
        return fold.nearestCopy(BlockPos.ZERO, delta);
    }

    private SeamDelta() {
    }
}
