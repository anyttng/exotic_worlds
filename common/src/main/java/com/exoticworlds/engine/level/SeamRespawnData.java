package com.exoticworlds.engine.level;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelData;

public final class SeamRespawnData {
    public static LevelData.RespawnData insideBounds(
            @Nullable MinecraftServer server, LevelData.RespawnData respawnData) {
        WorldFold fold = WorldLoopAttachments.foldIfLevel(
                server == null ? null : server.getLevel(respawnData.dimension()));
        BlockPos pos = respawnData.pos();
        BlockPos folded = fold.fold(pos);
        if (folded == pos) {
            return respawnData;
        }

        return new LevelData.RespawnData(
                GlobalPos.of(respawnData.dimension(), folded),
                respawnData.yaw(),
                respawnData.pitch());
    }

    private SeamRespawnData() {
    }
}
