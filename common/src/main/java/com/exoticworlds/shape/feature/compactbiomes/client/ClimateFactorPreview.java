package com.exoticworlds.shape.feature.compactbiomes.client;

import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.gen.DensityNoises;
import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;

final class ClimateFactorPreview {
    private static final double CLIMATE_XZ_SCALE = 0.25;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final boolean IS_CLIMATE_FIELD = true;

    static OptionalDouble temperatureFactor(Screen parent, ShapeOptions options, FlatShape spans) {
        NoiseFunction temperature = temperatureNoise(parent);
        if (temperature == null) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(ClimateCompression.factor(
                WorldFolds.of(spans, options),
                IS_CLIMATE_FIELD,
                temperature.noise(),
                CLIMATE_XZ_SCALE,
                HORIZONTAL_SHARE));
    }

    private static @Nullable NoiseFunction temperatureNoise(Screen parent) {
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

        return DensityNoises.firstNoiseFunctionUnder(noise.generatorSettings().value().noiseRouter().temperature());
    }

    private ClimateFactorPreview() {
    }
}
