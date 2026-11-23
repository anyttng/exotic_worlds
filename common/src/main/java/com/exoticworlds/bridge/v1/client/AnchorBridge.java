package com.exoticworlds.bridge.v1.client;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.client.engine.ClientFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class AnchorBridge {

    public static @Nullable BlockPos nearestToPlayer(@Nullable BlockPos target) {
        return ClientFrame.nearestToPlayer(target);
    }

    public static @Nullable Vec3 nearestToPlayer(@Nullable Vec3 target) {
        return ClientFrame.nearestToPlayer(target);
    }

    public static @Nullable ChunkPos nearestToPlayer(@Nullable ChunkPos target) {
        return ClientFrame.nearestToPlayer(target);
    }

    public static @Nullable Vec3 nearestToCamera(@Nullable Vec3 target) {
        return ClientFrame.nearestToCamera(target);
    }

    public static @Nullable BlockPos heldCopy(BlockPos canonical) {
        return ClientFrame.heldCopy(canonical);
    }

    public static @Nullable ChunkPos heldCopy(ChunkPos canonical, Predicate<ChunkPos> holds) {
        return ClientFrame.heldCopy(canonical, holds);
    }

    private AnchorBridge() {
    }
}
