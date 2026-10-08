package com.exoticworlds.compat;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.shape.climate.ClimateCompression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;

public final class LevelClimateCompression {
    public static DensityFunction temperatureOf(@Nullable ServerLevel level) {
        return temperatureOf(routerTemperatureOf(level), overworldTemperatureOf(level));
    }

    public static DensityFunction temperatureOf(ChunkGenerator own, @Nullable ChunkGenerator overworld) {
        return temperatureOf(routerTemperatureOf(own), routerTemperatureOf(overworld));
    }

    static DensityFunction temperatureOf(DensityFunction own, DensityFunction overworld) {
        return ClimateCompression.climateNoiseOf(own) != null ? own : overworld;
    }

    static DensityFunction overworldTemperatureOf(@Nullable ServerLevel level) {
        return routerTemperatureOf(level != null ? level.getServer().overworld() : null);
    }

    static DensityFunction routerTemperatureOf(@Nullable ServerLevel level) {
        return routerTemperatureOf(level != null ? level.getChunkSource().getGenerator() : null);
    }

    private static DensityFunction routerTemperatureOf(@Nullable ChunkGenerator generator) {
        if (generator instanceof NoiseBasedChunkGenerator noise) {
            return noise.generatorSettings().value().noiseRouter().temperature();
        }

        return DensityFunctions.zero();
    }

    public static double factor(WorldFold fold, DensityFunction temperature) {
        NoiseFunction climate = ClimateCompression.climateNoiseOf(temperature);
        return climate == null
                ? ClimateScaleCompression.NO_COMPRESSION
                : ClimateCompression.factor(fold, climate.noise(), climate.xzScale(),
                        GenerationTransformerContext.verticalShare(climate.xzScale(), climate.yScale()));
    }

    private LevelClimateCompression() {
    }
}
