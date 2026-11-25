package com.exoticworlds.bridge.v1.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.gen.TerrainSnapshot;
import com.exoticworlds.core.feature.CarvedChunkFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.shape.feature.floatingcrumbs.FloatingCrumbs;
import com.exoticworlds.engine.gen.terrain.TerrainCeiling;
import com.exoticworlds.shape.feature.floatingcrumbs.TerrainMask;
import com.exoticworlds.engine.noise.GenerationFoldContext;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

public final class ProbeBridge {
    private static final int WINDOW_SNAPSHOTS = 9;

    public static RandomState randomState(ServerLevel level, long seed) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            throw new IllegalArgumentException(
                    "The level's generator is not noise based: " + generator.getClass().getName());
        }

        @Nullable WorldFold fold = WorldLoopAttachments.getWrappedFold(level);
        NoiseGeneratorSettings settings = noise.generatorSettings().value();
        NoiseGeneratorSettings shaped = fold != null ? TerrainCeiling.withCeiling(settings) : settings;
        return GenerationFoldContext.withRouterBuild(fold, Registries.levelToLevelStem(level.dimension()),
                GeneratorShapes.activeFeatures(generator, level.dimension()),
                () -> RandomState.create(shaped, level.registryAccess().lookupOrThrow(Registries.NOISE), seed));
    }

    public static void withFold(ServerLevel level, Runnable body) {
        GenerationFoldContext.runWithFold(WorldLoopAttachments.getFold(level), body);
    }

    public static void sweepCrumbs(ServerLevel level, ChunkAccess chunk) {
        if (ShapeFeatures.onLevel(level, CarvedChunkFeature.class).contains(FloatingCrumbs.FEATURE)) {
            FloatingCrumbs.sweep(level, chunk);
        }
    }

    public static @Nullable TerrainSnapshot snapshotOf(ChunkAccess chunk) {
        TerrainMask mask = FloatingCrumbs.getTerrainMask(chunk);
        return mask == null ? null : new TerrainSnapshotView(mask);
    }

    public static long[] borderCrumbs(TerrainSnapshot[] window) {
        return FloatingCrumbs.crumbPositions(masksOf(window));
    }

    public static long[] clearBorderCrumbs(ChunkAccess chunk, TerrainSnapshot[] window, LevelReader reader) {
        return FloatingCrumbs.clearBorderCrumbs(chunk, masksOf(window), reader);
    }

    private static TerrainMask[] masksOf(TerrainSnapshot[] window) {
        if (window.length != WINDOW_SNAPSHOTS) {
            throw new IllegalArgumentException("A 3 x 3 window takes nine snapshots, not " + window.length);
        }

        TerrainMask[] masks = new TerrainMask[window.length];
        for (int i = 0; i < window.length; i++) {
            masks[i] = window[i] instanceof TerrainSnapshotView view ? view.mask() : null;
        }

        return masks;
    }

    private ProbeBridge() {
    }
}
