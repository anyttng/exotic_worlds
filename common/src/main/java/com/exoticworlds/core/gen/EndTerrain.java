package com.exoticworlds.core.gen;

import com.exoticworlds.core.world.StemTerrain;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;

public final class EndTerrain {
    public static boolean isEndTerrain(ResourceKey<LevelStem> stem, ChunkGenerator generator) {
        return StemTerrain.ofKey(stem).isEndTerrain() || isEndTerrain(generator);
    }

    public static boolean isEndTerrain(ChunkGenerator generator) {
        return generator instanceof NoiseBasedChunkGenerator noise
                && isEndTerrain(noise.generatorSettings().value().noiseRouter());
    }

    public static boolean isEndTerrain(NoiseRouter router) {
        for (RouterField field : RouterField.values()) {
            if (hasEndIslands(field.getFunction(router))) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasEndIslands(DensityFunction function) {
        boolean[] isFound = {false};
        function.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                isFound[0] |= input instanceof DensityFunctions.EndIslandDensityFunction;
                return input;
            }
        });

        return isFound[0];
    }

    private EndTerrain() {
    }
}
