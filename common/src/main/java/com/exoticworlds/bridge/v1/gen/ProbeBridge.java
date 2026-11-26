package com.exoticworlds.bridge.v1.gen;

import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.gen.RouterBuild;
import com.exoticworlds.engine.noise.GenerationFoldContext;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public final class ProbeBridge {

    public static RandomState randomState(ServerLevel level, long seed) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            throw new IllegalArgumentException(
                    "The level's generator is not noise based: " + generator.getClass().getName());
        }

        RouterBuild build = RouterBuild.of(generator, level.dimension(), noise.generatorSettings().value());
        return GenerationFoldContext.withRouterBuild(build.fold(), build.dimension(), build.features(),
                () -> RandomState.create(build.settings(), level.registryAccess().lookupOrThrow(Registries.NOISE),
                        seed));
    }

    public static void withFold(ServerLevel level, Runnable body) {
        GenerationFoldContext.runWithFold(WorldLoopAttachments.getFold(level), body);
    }

    private ProbeBridge() {
    }
}
