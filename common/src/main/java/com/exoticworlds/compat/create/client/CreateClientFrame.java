package com.exoticworlds.compat.create.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public final class CreateClientFrame {
    public static BlockPos nearestCopy(@Nullable Level level, BlockPos canonical) {
        return level != null && ClientFrame.isClientLevel(level) ? inViewerFrame(canonical) : canonical;
    }

    public static BlockPos inViewerFrame(Vec3i canonical) {
        return ClientFrame.nearestToPlayer(NearestCopy.asBlockPos(canonical));
    }

    private CreateClientFrame() {
    }
}
