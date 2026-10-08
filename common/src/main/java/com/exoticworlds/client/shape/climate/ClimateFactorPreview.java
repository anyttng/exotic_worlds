package com.exoticworlds.client.shape.climate;

import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.exoticworlds.shape.noise.DensityNoises;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

final class ClimateFactorPreview {
    private static final double CLIMATE_XZ_SCALE = 0.25;

    private static final double HORIZONTAL_SHARE = 0.0;

    static OptionalDouble temperatureFactor(Screen parent, GenerationOptions generationOptions, LoopSpans spans) {
        DensityNoises.ScaledNoise temperature = temperatureNoise(parent);
        if (temperature == null) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(ClimateCompression.factor(
                WorldFolds.of(FlatShape.of(spans), generationOptions),
                temperature.noise(),
                CLIMATE_XZ_SCALE,
                HORIZONTAL_SHARE));
    }

    private static DensityNoises.@Nullable ScaledNoise temperatureNoise(Screen parent) {
        if (!(parent instanceof CreateWorldScreen create)) {
            return null;
        }

        ChunkGenerator overworld = create.getUiState().getSettings().selectedDimensions()
                .get(LevelStem.OVERWORLD)
                .map(LevelStem::generator)
                .orElse(null);
        if (!(overworld instanceof NoiseBasedChunkGenerator noise)) {
            return null;
        }

        return ClimateCompression.climateNoiseOf(noise.generatorSettings().value().noiseRouter().temperature());
    }

    private ClimateFactorPreview() {
    }
}
