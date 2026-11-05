package com.exoticworlds.engine.level;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.mixin.DelegateBorderChangeListenerAccessor;
import com.exoticworlds.mixin.MinecraftServerAccessor;
import com.exoticworlds.mixin.WorldBorderAccessor;
import com.exoticworlds.platform.Platforms;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.WorldData;

public final class LevelRebuild {
    private static final ChunkProgressListener NO_PROGRESS = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(ChunkPos center) {
        }

        @Override
        public void onStatusChange(ChunkPos pos, @Nullable ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    public static ServerLevel rebuild(MinecraftServer server, ServerLevel old) {
        ResourceKey<Level> dimension = old.dimension();
        LevelStem stem = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
                .getOrThrow(Registries.levelToLevelStem(dimension));
        close(server, old);
        ServerLevel fresh = create(server, dimension, stem);
        ((MinecraftServerAccessor) server).exotic_worlds$levels().put(dimension, fresh);
        Platforms.get().postLevelLoad(server, fresh);
        followOverworldBorder(server.overworld().getWorldBorder(), old.getWorldBorder(), fresh.getWorldBorder());
        return fresh;
    }

    private static void close(MinecraftServer server, ServerLevel level) {
        try {
            level.close();
        } catch (IOException failed) {
            throw new UncheckedIOException("Could not close " + level.dimension().location() + " to rebuild it", failed);
        }

        Platforms.get().postLevelUnload(server, level);
    }

    private static ServerLevel create(MinecraftServer server, ResourceKey<Level> dimension, LevelStem stem) {
        MinecraftServerAccessor access = (MinecraftServerAccessor) server;
        WorldData worldData = server.getWorldData();
        return new ServerLevel(server, access.exotic_worlds$executor(), access.exotic_worlds$storageSource(),
                new DerivedLevelData(worldData, worldData.overworldData()), dimension, stem, NO_PROGRESS,
                worldData.isDebugWorld(), BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()), List.of(),
                false, server.overworld().getRandomSequences());
    }

    private static void followOverworldBorder(WorldBorder overworld, WorldBorder old, WorldBorder fresh) {
        for (BorderChangeListener listener : ((WorldBorderAccessor) overworld).exotic_worlds$listeners()) {
            if (listener instanceof BorderChangeListener.DelegateBorderChangeListener delegate
                    && ((DelegateBorderChangeListenerAccessor) delegate).exotic_worlds$target() == old) {
                overworld.removeListener(listener);
            }
        }

        overworld.addListener(new BorderChangeListener.DelegateBorderChangeListener(fresh));
        fresh.applySettings(overworld.createSettings());
    }

    private LevelRebuild() {
    }
}
