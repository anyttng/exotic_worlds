package com.exoticworlds.core.feature;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface ChunkDataFeature extends LevelFeature {
    String getDataKey();

    @Nullable CompoundTag getSavedData(ServerLevel level, ChunkPos pos);

    void restoreData(ServerLevel level, ChunkAccess chunk, CompoundTag data);

    void releaseData(ServerLevel level, ChunkPos pos);
}
