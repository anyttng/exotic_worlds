package com.exoticworlds.engine.level;

import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class SeamRespawnData {
    public static BlockPos insideBounds(ServerLevel level, BlockPos pos) {
        return WorldLoopAttachments.foldOf(level).fold(pos);
    }

    private SeamRespawnData() {
    }
}
