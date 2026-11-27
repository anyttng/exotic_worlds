package com.exoticworlds.compat.worldgen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FoldGenerators;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

public final class FoldCompression {
    public static double of(WorldFold fold) {
        ChunkGenerator generator = FoldGenerators.getGenerator(fold);
        if (generator == null) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        return LevelClimateCompression.factor(fold,
                LevelClimateCompression.temperatureOf(generator, getOverworldGenerator()));
    }

    private static @Nullable ChunkGenerator getOverworldGenerator() {
        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return null;
        }

        LevelStem overworld = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM)
                .getValue(LevelStem.OVERWORLD);
        return overworld != null ? overworld.generator() : null;
    }

    private FoldCompression() {
    }
}
