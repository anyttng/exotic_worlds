package com.exoticworlds.bridge.v2.feature;

import com.exoticworlds.api.v2.feature.ChunkSlotV2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

final class ChunkSlotView extends MomentView implements ChunkSlotV2 {
    private final ServerLevel level;
    private final ChunkPos pos;

    ChunkSlotView(ServerLevel level, ChunkPos pos) {
        super(level);
        this.level = level;
        this.pos = pos;
    }

    @Override
    public ServerLevel getLevel() {
        return this.level;
    }

    @Override
    public ChunkPos getPos() {
        return this.pos;
    }
}
