package com.exoticworlds.engine.gen;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.engine.gen.terrain.TerrainCeiling;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public record RouterBuild(@Nullable WorldFold fold, ResourceKey<LevelStem> dimension, List<GenerationFeature> features,
        NoiseGeneratorSettings settings) {

    public static RouterBuild of(ChunkGenerator generator, ResourceKey<Level> level, NoiseGeneratorSettings settings) {
        WorldFold fold = GeneratorShapes.getWrappedFold(generator);
        return new RouterBuild(fold, Registries.levelToLevelStem(level),
                GeneratorShapes.activeFeatures(generator, level), TerrainCeiling.shaped(fold, settings));
    }
}
