package com.exoticworlds.engine.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.world.CarriedShape;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

public final class LegacyGenerators {
    public static final String TOROIDAL_ID = "toroidal";
    public static final String TOROIDAL_FLAT_ID = "toroidal_flat";

    private static final String BIOME_SOURCE_KEY = "biome_source";
    private static final String SETTINGS_KEY = "settings";

    private static final MapCodec<CarriedShape> CARRIED_CODEC = CarriedShape.mapCodec(CarriedShape.NO_KEY_PREFIX);

    public static final MapCodec<NoiseBasedChunkGenerator> TOROIDAL_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf(BIOME_SOURCE_KEY).forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf(SETTINGS_KEY).forGetter(NoiseBasedChunkGenerator::generatorSettings),
                    CARRIED_CODEC.forGetter(LegacyGenerators::getStamp)
            ).apply(instance, instance.stable((biomeSource, settings, carried) ->
                    stamped(new NoiseBasedChunkGenerator(biomeSource, settings), carried))));

    public static final MapCodec<FlatLevelSource> TOROIDAL_FLAT_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    FlatLevelGeneratorSettings.CODEC.fieldOf(SETTINGS_KEY).forGetter(FlatLevelSource::settings),
                    CARRIED_CODEC.forGetter(LegacyGenerators::getStamp)
            ).apply(instance, instance.stable((settings, carried) -> stamped(new FlatLevelSource(settings), carried))));

    private static <G extends ChunkGenerator> G stamped(G generator, CarriedShape carried) {
        ((ShapeStamp) generator).exotic_worlds$stamp(carried);
        return generator;
    }

    private static @Nullable CarriedShape getStamp(ChunkGenerator generator) {
        return ((ShapeStamp) generator).exotic_worlds$getCarriedShape();
    }

    private LegacyGenerators() {
    }
}
