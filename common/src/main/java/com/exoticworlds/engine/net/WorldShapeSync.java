package com.exoticworlds.engine.net;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.platform.Platforms;

import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public final class WorldShapeSync {
    public static void sendOnLevelChange(ServerPlayer player, ServerLevel from) {
        send(player, player.level());
        PlayerList players = player.level().getServer().getPlayerList();
        if (limitIn(from, players.getViewDistance()) != distanceFor(player, players.getViewDistance())) {
            sendViewDistance(player);
        }

        if (limitIn(from, players.getSimulationDistance()) != distanceFor(player, players.getSimulationDistance())) {
            sendSimulationDistance(player);
        }
    }

    public static int distanceFor(ServerPlayer player, int distance) {
        return limitIn(player.level(), distance);
    }

    public static boolean limitsAnyPlayer(PlayerList players, int distance) {
        for (ServerPlayer player : players.getPlayers()) {
            if (distanceFor(player, distance) != distance) {
                return true;
            }
        }

        return false;
    }

    public static void sendViewDistance(ServerPlayer player) {
        int viewDistance = player.level().getServer().getPlayerList().getViewDistance();
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(distanceFor(player, viewDistance)));
    }

    public static void sendSimulationDistance(ServerPlayer player) {
        int simulationDistance = player.level().getServer().getPlayerList().getSimulationDistance();
        player.connection.send(new ClientboundSetSimulationDistancePacket(distanceFor(player, simulationDistance)));
    }

    public static void sendAllTo(ServerPlayer player) {
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            send(player, level);
        }
    }

    public static void sendToAll(ServerLevel level) {
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(level);
        FlatShape shape = carried == null ? FlatShape.rectangle() : carried.shape();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            Platforms.get().sendWorldShape(player, level.dimension(), shape);
        }
    }

    private static int limitIn(ServerLevel level, int distance) {
        return WorldLoopAttachments.foldOf(level).limitViewDistance(distance);
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
