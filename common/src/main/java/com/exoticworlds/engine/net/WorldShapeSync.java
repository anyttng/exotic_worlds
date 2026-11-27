package com.exoticworlds.engine.net;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.fold.WorldLoopAttachments;
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

    public static int distanceFor(ServerPlayer player, int distanceChunks) {
        return limitIn(player.level(), distanceChunks);
    }

    public static boolean isLimitingAnyPlayer(PlayerList players, int distanceChunks) {
        for (ServerPlayer player : players.getPlayers()) {
            if (distanceFor(player, distanceChunks) != distanceChunks) {
                return true;
            }
        }

        return false;
    }

    public static void sendViewDistance(ServerPlayer player) {
        int viewDistanceChunks = player.level().getServer().getPlayerList().getViewDistance();
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(distanceFor(player, viewDistanceChunks)));
    }

    public static void sendSimulationDistance(ServerPlayer player) {
        int simulationDistanceChunks = player.level().getServer().getPlayerList().getSimulationDistance();
        player.connection.send(new ClientboundSetSimulationDistancePacket(distanceFor(player, simulationDistanceChunks)));
    }

    public static void sendAllTo(ServerPlayer player) {
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            send(player, level);
        }
    }

    public static void sendToAll(ServerLevel level) {
        CarriedShape carried = GeneratorShapes.getCarriedShape(level);
        ShapeModel shape = carried == null ? FlatShape.rectangle() : carried.getShape();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            Platforms.get().sendWorldShape(player, level.dimension(), shape);
        }
    }

    private static int limitIn(ServerLevel level, int distanceChunks) {
        return WorldLoopAttachments.getFold(level).limitViewDistance(distanceChunks);
    }

    private static void send(ServerPlayer player, ServerLevel level) {
        CarriedShape carried = GeneratorShapes.getCarriedShape(level);
        if (carried != null) {
            Platforms.get().sendWorldShape(player, level.dimension(), carried.getShape());
        }
    }

    private WorldShapeSync() {
    }
}
