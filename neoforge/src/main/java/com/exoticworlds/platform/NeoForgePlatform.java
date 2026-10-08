package com.exoticworlds.platform;

import java.nio.file.Path;
import java.util.function.IntFunction;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.engine.net.PositionRowsPayload;
import com.exoticworlds.engine.net.WrappingSettingsPayload;

import io.netty.buffer.Unpooled;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgePlatform implements Platform {
    private final ModContainer modContainer;

    public NeoForgePlatform(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public String modVersion() {
        return modContainer.getModInfo().getVersion().toString();
    }

    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public String loaderVersion() {
        return NeoForgeVersion.getVersion();
    }

    @Override
    public void sendWorldShape(ServerPlayer player, ResourceKey<Level> dimension, FlatShape shape) {
        if (player.connection.hasChannel(WrappingSettingsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new WrappingSettingsPayload(dimension, shape));
        }
    }

    @Override
    public void sendPositionRows(ServerPlayer player, PositionRowsPayload rows) {
        if (player.connection.hasChannel(PositionRowsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, rows);
        }
    }

    @Override
    public IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player) {
        return capacity -> new RegistryFriendlyByteBuf(
                Unpooled.buffer(capacity), player.registryAccess(), player.connection.getConnectionType());
    }

    @Override
    public LevelStem withGenerator(LevelStem stem, ChunkGenerator generator) {
        return new LevelStem(stem.type(), generator);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
