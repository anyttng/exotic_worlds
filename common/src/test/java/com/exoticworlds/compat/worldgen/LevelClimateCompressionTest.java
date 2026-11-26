package com.exoticworlds.compat.worldgen;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

class LevelClimateCompressionTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createWorldLookup();

    private static final DensityFunction OVERWORLD_TEMPERATURE = temperatureOf(NoiseGeneratorSettings.OVERWORLD);

    private static final DensityFunction END_TEMPERATURE = temperatureOf(NoiseGeneratorSettings.END);

    private static final List<ResourceKey<NoiseGeneratorSettings>> CLIMATE_ROUTERS =
            List.of(NoiseGeneratorSettings.LARGE_BIOMES, NoiseGeneratorSettings.NETHER);

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

    private static DensityFunction temperatureOf(ResourceKey<NoiseGeneratorSettings> settings) {
        return LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings).value().noiseRouter()
                .temperature();
    }
}
