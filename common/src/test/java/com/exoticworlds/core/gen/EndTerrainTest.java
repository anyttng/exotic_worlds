package com.exoticworlds.core.gen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;

class EndTerrainTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createWorldLookup();

    private static final List<ResourceKey<NoiseGeneratorSettings>> NOT_END = List.of(NoiseGeneratorSettings.OVERWORLD,
            NoiseGeneratorSettings.LARGE_BIOMES, NoiseGeneratorSettings.AMPLIFIED, NoiseGeneratorSettings.NETHER,
            NoiseGeneratorSettings.CAVES, NoiseGeneratorSettings.FLOATING_ISLANDS);

    @Test
    void theEndRouterIsEndTerrain() {
        assertTrue(EndTerrain.isEndTerrain(routerOf(NoiseGeneratorSettings.END)));
    }

    @Test
    void theOtherVanillaRoutersAreNot() {
        for (ResourceKey<NoiseGeneratorSettings> settings : NOT_END) {
            assertFalse(EndTerrain.isEndTerrain(routerOf(settings)), settings.identifier().toString());
        }
    }

    private static NoiseRouter routerOf(ResourceKey<NoiseGeneratorSettings> settings) {
        return LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings).value().noiseRouter();
    }
}
