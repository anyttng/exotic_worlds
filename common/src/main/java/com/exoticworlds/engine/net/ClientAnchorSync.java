package com.exoticworlds.engine.net;

import java.util.Objects;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.ClientPosition.BorderCenter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;

public final class ClientAnchorSync {
    public static void refresh(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(level);
        if (fold == null) {
            return;
        }

        ClientPosition clientPosition = ClientPosition.of(player);
        if (!clientPosition.describes(level.dimension())) {
            return;
        }

        clientPosition.followCamera(player);
        clientPosition.dropLeftCamera();
        refreshSpawn(player, level, fold, clientPosition);
        refreshBorderCenter(player, level, fold, clientPosition);
        refreshCacheCenter(player, fold, clientPosition);
    }

    private static void refreshSpawn(ServerPlayer player, ServerLevel level, WorldFold fold,
            ClientPosition clientPosition) {
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return;
        }

        BlockPos held = clientPosition.heldSpawn();
        BlockPos spawnPos = level.getSharedSpawnPos();
        BlockPos want = TranslationContext.nearestCopy(fold, clientPosition, spawnPos);
        if (Objects.equals(want, held)) {
            return;
        }

        player.connection.send(
                new ClientboundSetDefaultSpawnPositionPacket(spawnPos, level.getSharedSpawnAngle()));
    }

    private static void refreshBorderCenter(ServerPlayer player, ServerLevel level, WorldFold fold,
            ClientPosition clientPosition) {
        WorldBorder border = level.getWorldBorder();
        BorderCenter held = clientPosition.heldBorderCenter();
        BorderCenter want = ClientboundPacketRewriters.nearestCopyCenter(fold, clientPosition,
                new BorderCenter(border.getCenterX(), border.getCenterZ()));
        if (want.equals(held)) {
            return;
        }

        player.connection.send(new ClientboundSetBorderCenterPacket(border));
    }

    private static void refreshCacheCenter(ServerPlayer player, WorldFold fold, ClientPosition clientPosition) {
        ChunkPos held = clientPosition.heldCacheCenter();
        if (held == null || !(player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view)) {
            return;
        }

        ChunkPos want = TranslationContext.nearestCopy(fold, clientPosition, view.center());
        if (want.equals(held)) {
            return;
        }

        player.connection.send(new ClientboundSetChunkCacheCenterPacket(view.center().x, view.center().z));
    }

    private ClientAnchorSync() {
    }
}
