package com.exoticworlds.compat;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

public final class FoldCompression {
    public static double of(WorldFold fold) {
        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        Registry<LevelStem> stems = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
        for (LevelStem stem : stems) {
            ChunkGenerator generator = stem.generator();
            if (ShapedChunkGenerator.transformerOf(generator) == fold) {
                LevelStem overworld = stems.getValue(LevelStem.OVERWORLD);
                return LevelClimateCompression.factor(fold, LevelClimateCompression.temperatureOf(generator,
                        overworld != null ? overworld.generator() : null));
            }
        }

        return ClimateScaleCompression.NO_COMPRESSION;
    }

    private FoldCompression() {
    }
}
