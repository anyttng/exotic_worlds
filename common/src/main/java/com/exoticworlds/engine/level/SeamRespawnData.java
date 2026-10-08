package com.exoticworlds.engine.level;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class SeamRespawnData {
    public static BlockPos insideBounds(ServerLevel level, BlockPos pos) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? pos : transformer.fold(pos);
    }

    private SeamRespawnData() {
    }
}
