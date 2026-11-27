package com.exoticworlds.engine.gen;

import com.exoticworlds.accessors.BaseHeightsHolder;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FoldGenerators;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

public final class LoopedChunkGenerator extends NoiseBasedChunkGenerator implements ShapedChunkGenerator {
    private static final String BIOME_SOURCE_KEY = "biome_source";

    public static final MapCodec<LoopedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf(BIOME_SOURCE_KEY).forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf(SETTINGS_KEY).forGetter(NoiseBasedChunkGenerator::generatorSettings),
                    CARRIED_CODEC.forGetter(LoopedChunkGenerator::carriedShape)
            ).apply(instance, instance.stable(LoopedChunkGenerator::new)));

    private final CarriedShape carriedShape;

    public LoopedChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
            CarriedShape carriedShape) {
        super(biomeSource, settings);
        this.carriedShape = carriedShape;
        FoldGenerators.fileFold(this, carriedShape);
    }

    @Override
    public CarriedShape carriedShape() {
        return this.carriedShape;
    }

    @Override
    public ChunkGenerator unshaped() {
        return new NoiseBasedChunkGenerator(getBiomeSource(), generatorSettings());
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor,
            RandomState randomState) {
        BaseHeights heights = ((BaseHeightsHolder) (Object) randomState).exotic_worlds$baseHeights();
        if (heights == null) {
            return super.getBaseHeight(x, z, type, heightAccessor, randomState);
        }

        return heights.heightAt(x, z, type, () -> super.getBaseHeight(x, z, type, heightAccessor, randomState));
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}
