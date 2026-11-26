package com.exoticworlds.core.gen;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.densityfunction.generator.EndIslandFunction;

public final class EndTerrain {
    public static boolean isEndTerrain(ChunkGenerator generator) {
        return generator instanceof NoiseBasedChunkGenerator noise
                && isEndTerrain(noise.generatorSettings().value().noiseRouter());
    }

    public static boolean isEndTerrain(NoiseRouter router) {
        for (RouterField field : RouterField.values()) {
            if (DensityFunctionNodes.first(field.getFunction(router), EndIslandFunction.class::isInstance) != null) {
                return true;
            }
        }

        return false;
    }

    private EndTerrain() {
    }
}
