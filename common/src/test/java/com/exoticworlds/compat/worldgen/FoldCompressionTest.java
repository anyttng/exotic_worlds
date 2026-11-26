package com.exoticworlds.compat.worldgen;

import java.util.List;

import static com.exoticworlds.engine.noise.ClimateScaleCompression.NO_COMPRESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FoldGenerators;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.feature.compactbiomes.ClimateScale;
import com.exoticworlds.shape.feature.compactbiomes.CompactBiomes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

class FoldCompressionTest {
    private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();

    private static final int TINY_CHUNK_MIN = -16;

    private static final int TINY_CHUNK_MAX = 16;

    @Test
    void aShapedGeneratorsFoldTakesItsFactorInsideItsRouterBuildWithNoServer() {
        CarriedShape carried = compactTiny();
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(LOOKUP.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                LOOKUP.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD));
        FoldGenerators.fileFold(generator, carried);
        WorldFold fold = carried.getFold();
        List<GenerationFeature> features = List.of(CompactBiomes.FEATURE);
        double expected = GenerationFoldContext.withRouterBuild(fold, LevelStem.OVERWORLD, features,
                () -> LevelClimateCompression.factor(fold, LevelClimateCompression.temperatureOf(generator, null)));

        assertNull(CurrentServer.get());
        assertTrue(expected > NO_COMPRESSION, String.valueOf(expected));
        assertEquals(expected, GenerationFoldContext.withRouterBuild(fold, LevelStem.OVERWORLD, features,
                () -> FoldCompression.of(fold)));
    }

    @Test
    void aFoldNoGeneratorCarriesIsNotCompressed() {
        assertEquals(NO_COMPRESSION, FoldCompression.of(compactTiny().getFold()));
    }

    private static CarriedShape compactTiny() {
        return new CarriedShape(null,
                ShapeFixtures.torus(
                        ShapeFixtures.loopedBounds(TINY_CHUNK_MIN, TINY_CHUNK_MAX, TINY_CHUNK_MIN, TINY_CHUNK_MAX)),
                ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.STRONG));
    }
}
