package com.exoticworlds.compat.jmi;

import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;

public final class JmiFold {
    public static ChunkPos foldChunk(ChunkPos chunk) {
        return FtbChunksFold.foldedChunkPos(chunk);
    }

    public static ChunkPos seatNearPlayer(ChunkPos folded) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return folded;
        }

        return FtbChunksFold.nearestChunk(foldChunk(player.chunkPosition()), folded);
    }

    private JmiFold() {
    }
}
