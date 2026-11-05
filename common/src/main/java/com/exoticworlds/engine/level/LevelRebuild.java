package com.exoticworlds.engine.level;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.exoticworlds.mixin.MinecraftServerAccessor;
import com.exoticworlds.platform.Platforms;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.WorldData;

public final class LevelRebuild {
    public static ServerLevel rebuild(MinecraftServer server, ServerLevel old) {
        ResourceKey<Level> dimension = old.dimension();
        LevelStem stem = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM)
                .getValueOrThrow(Registries.levelToLevelStem(dimension));
        close(server, old);
        ServerLevel fresh = create(server, dimension, stem);
        ((MinecraftServerAccessor) server).exotic_worlds$levels().put(dimension, fresh);
        Platforms.get().postLevelLoad(server, fresh);
        fresh.getWorldBorder().setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
        server.getPlayerList().addWorldborderListener(fresh);
        return fresh;
    }

    private static void close(MinecraftServer server, ServerLevel level) {
        try {
            level.close();
        } catch (IOException failed) {
            throw new UncheckedIOException("Could not close " + level.dimension().identifier() + " to rebuild it", failed);
        }

        Platforms.get().postLevelUnload(server, level);
    }

    private static ServerLevel create(MinecraftServer server, ResourceKey<Level> dimension, LevelStem stem) {
        MinecraftServerAccessor access = (MinecraftServerAccessor) server;
        WorldData worldData = server.getWorldData();
        return new ServerLevel(server, access.exotic_worlds$executor(), access.exotic_worlds$storageSource(),
                new DerivedLevelData(worldData, worldData.overworldData()), dimension, stem, worldData.isDebugWorld(),
                BiomeManager.obfuscateSeed(server.getWorldGenSettings().options().seed()), List.of(), false);
    }

    private LevelRebuild() {
    }
}
