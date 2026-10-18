package com.exoticworlds.accessors;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkResender {
    void exotic_worlds$dropTrackedChunks(ServerPlayer player);

    void exotic_worlds$resendTrackedChunks(ServerPlayer player);

    void exotic_worlds$dropChunks(ServerPlayer player, List<ChunkPos> chunks);

    void exotic_worlds$resendChunks(ServerPlayer player, List<ChunkPos> chunks);
}
