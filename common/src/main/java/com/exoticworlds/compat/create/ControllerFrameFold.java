package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.create.client.CreateClientFrame;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class ControllerFrameFold {
    public static BlockPos inFrameOf(@Nullable Level level, BlockPos controller) {
        if (level == null) {
            return controller;
        }

        if (level.isClientSide) {
            return CreateClientFrame.nearestCopy(level, controller);
        }

        return WorldLoopAttachments.foldOf(level).fold(controller);
    }

    private ControllerFrameFold() {
    }
}
