package com.exoticworlds.platform;

import java.nio.file.Path;
import java.util.function.IntFunction;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.engine.net.PositionRowsPayload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

public interface Platform {
    boolean isClient();

    String modVersion();

    String loaderName();

    String loaderVersion();

    void sendWorldShape(ServerPlayer player, ResourceKey<Level> dimension, FlatShape shape);

    void sendPositionRows(ServerPlayer player, PositionRowsPayload rows);

    IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player);

    LevelStem withGenerator(LevelStem stem, ChunkGenerator generator);

    Path configDir();
}
