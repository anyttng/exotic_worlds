package com.exoticworlds.compat.worldgen;

import static com.exoticworlds.engine.noise.ClimateScaleCompression.NO_COMPRESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.engine.gen.LoopedChunkGenerator;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

class FoldCompressionTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();

    private static final int TINY_CHUNK_MIN = -16;

    private static final int TINY_CHUNK_MAX = 16;

    @Test
    void aShapedGeneratorsFoldTakesItsFactorWithNoServer() {
        CarriedShape carried = compactTiny();
        LoopedChunkGenerator generator = new LoopedChunkGenerator(
                new FixedBiomeSource(LOOKUP.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD),
                carried);
        double expected = LevelClimateCompression.factor(carried.fold(),
                LevelClimateCompression.temperatureOf(generator, null));

        assertNull(CurrentServer.get());
        assertTrue(expected > NO_COMPRESSION, String.valueOf(expected));
        assertEquals(expected, FoldCompression.of(carried.fold()));
    }

    @Test
    void aFoldNoGeneratorCarriesIsNotCompressed() {
        assertEquals(NO_COMPRESSION, FoldCompression.of(compactTiny().fold()));
    }

    private static CarriedShape compactTiny() {
        return new CarriedShape(
                ShapeFixtures.torus(
                        ShapeFixtures.loopedBounds(TINY_CHUNK_MIN, TINY_CHUNK_MAX, TINY_CHUNK_MIN, TINY_CHUNK_MAX)),
                ShapeOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.STRONG));
    }
}
