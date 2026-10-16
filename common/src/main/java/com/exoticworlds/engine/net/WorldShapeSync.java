package com.exoticworlds.engine.net;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.platform.Platforms;

import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WorldShapeSync {
    public static void sendTo(ServerPlayer player) {
        send(player, player.serverLevel());
        sendViewDistance(player);
        sendSimulationDistance(player);
    }

    public static int distanceFor(ServerPlayer player, int distance) {
        return WorldLoopAttachments.transformerOf(player.serverLevel()).limitViewDistance(distance);
    }

    public static void sendViewDistance(ServerPlayer player) {
        int viewDistance = player.serverLevel().getServer().getPlayerList().getViewDistance();
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(distanceFor(player, viewDistance)));
    }

    public static void sendSimulationDistance(ServerPlayer player) {
        int simulationDistance = player.serverLevel().getServer().getPlayerList().getSimulationDistance();
        player.connection.send(new ClientboundSetSimulationDistancePacket(distanceFor(player, simulationDistance)));
    }

    public static void sendAllTo(ServerPlayer player) {
        for (ServerLevel level : player.serverLevel().getServer().getAllLevels()) {
            send(player, level);
        }
    }

    private static void send(ServerPlayer player, ServerLevel level) {
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(level);
        if (carried != null) {
            Platforms.get().sendWorldShape(player, level.dimension(), carried.shape());
        }
    }

    private WorldShapeSync() {
    }
}
