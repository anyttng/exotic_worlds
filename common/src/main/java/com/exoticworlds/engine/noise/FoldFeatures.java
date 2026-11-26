package com.exoticworlds.engine.noise;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.engine.level.CurrentServer;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

public record FoldFeatures(ResourceKey<LevelStem> dimension, List<GenerationFeature> features) {
    public static @Nullable FoldFeatures of(WorldFold fold) {
        if (!fold.isWrapped()) {
            return null;
        }

        GenerationFoldContext.Context context = GenerationFoldContext.getContext();
        if (context.getRouterBuildFold() == fold) {
            return new FoldFeatures(context.getRouterBuildDimension(), context.getRouterBuildFeatures());
        }

        MinecraftServer server = CurrentServer.get();
        return server == null ? null : ofStems(fold, server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM));
    }

    private static @Nullable FoldFeatures ofStems(WorldFold fold, Registry<LevelStem> stems) {
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> stem : stems.entrySet()) {
            ChunkGenerator generator = stem.getValue().generator();
            if (GeneratorShapes.getFold(generator) == fold) {
                return new FoldFeatures(stem.getKey(),
                        GeneratorShapes.activeFeatures(generator, Registries.levelStemToLevel(stem.getKey())));
            }
        }

        return null;
    }
}
