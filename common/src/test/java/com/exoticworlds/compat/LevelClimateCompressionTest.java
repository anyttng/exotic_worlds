package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.DensityFunction;

class LevelClimateCompressionTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();

    private static final DensityFunction OVERWORLD_TEMPERATURE = temperatureOf(NoiseGeneratorSettings.OVERWORLD);

    private static final DensityFunction END_TEMPERATURE = temperatureOf(NoiseGeneratorSettings.END);

    private static final List<ResourceKey<NoiseGeneratorSettings>> CLIMATE_ROUTERS =
            List.of(NoiseGeneratorSettings.LARGE_BIOMES, NoiseGeneratorSettings.NETHER);

    private static final int TINY_CHUNK_MIN = -16;

    private static final int TINY_CHUNK_MAX = 16;

    private static final int CUSTOM_FACTOR = 3;

    private static final List<ClimateScale> COMPRESSING_SCALES =
            List.of(ClimateScale.AUTO, ClimateScale.STRONG, ClimateScale.custom(CUSTOM_FACTOR));

    private static final double NO_COMPRESSION = 1.0;

    private static final double FACTOR_TOLERANCE = 1.0E-9;

    @Test
    void aRouterWithoutAClimateNoiseTakesTheOverworldTemperature() {
        assertSame(OVERWORLD_TEMPERATURE,
                LevelClimateCompression.temperatureOf(END_TEMPERATURE, OVERWORLD_TEMPERATURE));
    }

    @Test
    void aRouterWithAClimateNoiseKeepsItsOwn() {
        for (ResourceKey<NoiseGeneratorSettings> settings : CLIMATE_ROUTERS) {
            DensityFunction own = temperatureOf(settings);
            assertSame(own, LevelClimateCompression.temperatureOf(own, OVERWORLD_TEMPERATURE),
                    settings.identifier().toString());
        }
    }

    @Test
    void theEndTakesTheOverworldFactorOnTheSameLap() {
        DensityFunction end = LevelClimateCompression.temperatureOf(END_TEMPERATURE, OVERWORLD_TEMPERATURE);
        for (ClimateScale scale : COMPRESSING_SCALES) {
            WorldFold fold = compressed(scale);
            double overworld = LevelClimateCompression.factor(fold, OVERWORLD_TEMPERATURE);
            String mode = scale.mode().getSerializedName();
            assertTrue(overworld > NO_COMPRESSION, "the overworld compresses in " + mode);
            assertEquals(overworld, LevelClimateCompression.factor(fold, end), FACTOR_TOLERANCE,
                    "the end " + mode + " on a tiny world");
        }
    }

    private static DensityFunction temperatureOf(ResourceKey<NoiseGeneratorSettings> settings) {
        return LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings).value().noiseRouter()
                .temperature();
    }

    private static WorldFold compressed(ClimateScale scale) {
        return WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(TINY_CHUNK_MIN, TINY_CHUNK_MAX, TINY_CHUNK_MIN, TINY_CHUNK_MAX)),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, scale));
    }
}
