package com.exoticworlds.bridge.v1.client;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.client.ClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        LocalPlayer player = Minecraft.getInstance().player;
        return ClientFrame.nearestCopy(player == null ? null : player.chunkPosition(), target);
    }

    public static @Nullable Vec3 nearestToCamera(@Nullable Vec3 target) {
        return ClientFrame.nearestToCamera(target);
    }

    public static @Nullable BlockPos heldCopy(BlockPos canonical) {
        return ClientFrame.heldCopy(canonical);
    }

    public static @Nullable ChunkPos heldCopy(ChunkPos canonical, Predicate<ChunkPos> holds) {
        WorldFold fold = ClientFrame.getFold();
        LocalPlayer player = Minecraft.getInstance().player;
        if (fold == null || player == null) {
            return canonical;
        }

        ChunkPos nearest = fold.nearestCopy(player.chunkPosition(), canonical);
        return holds.test(nearest) ? nearest : null;
    }

    private AnchorBridge() {
    }
}
