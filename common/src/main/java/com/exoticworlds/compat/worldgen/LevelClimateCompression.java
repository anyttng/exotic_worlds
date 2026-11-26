package com.exoticworlds.compat.worldgen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.engine.noise.ForeignFieldCompression;
import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;

public final class LevelClimateCompression {
    public static DensityFunction temperatureOf(ChunkGenerator own, @Nullable ChunkGenerator overworld) {
        return temperatureOf(getRouterTemperature(own), getRouterTemperature(overworld));
    }

    static DensityFunction temperatureOf(DensityFunction own, DensityFunction overworld) {
        return ClimateCompression.climateNoiseOf(own) != null ? own : overworld;
    }

    static DensityFunction getOverworldTemperature(@Nullable ServerLevel level) {
        return getRouterTemperature(level != null ? level.getServer().overworld() : null);
    }

    static DensityFunction getRouterTemperature(@Nullable ServerLevel level) {
        return getRouterTemperature(level != null ? level.getChunkSource().getGenerator() : null);
    }

    private static DensityFunction getRouterTemperature(@Nullable ChunkGenerator generator) {
        if (generator instanceof NoiseBasedChunkGenerator noise) {
            return noise.generatorSettings().value().noiseRouter().temperature();
        }

        return DensityFunctions.zero();
    }

    public static double factor(WorldFold fold, DensityFunction temperature) {
        NoiseFunction climate = ClimateCompression.climateNoiseOf(temperature);
        return climate == null
                ? ClimateScaleCompression.NO_COMPRESSION
                : ForeignFieldCompression.followingNoise(fold, climate.noise(), climate.xzScale(), climate.yScale());
    }

    private LevelClimateCompression() {
    }
}
