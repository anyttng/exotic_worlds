package com.exoticworlds.engine.noise;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseOffsets;
import com.exoticworlds.accessors.FoldedCompileSource;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

public record FoldedCompileContext(DensityFunction.CompileContext vanilla, WorldFold fold,
        ResourceKey<LevelStem> dimension, List<NoiseFeature> noiseFeatures, NoiseOffsets offsets,
        NoiseScaleLadder ladder, double xDivisor, double zDivisor) {
    public FoldedCompileContext(DensityFunction.CompileContext vanilla, WorldFold fold,
            ResourceKey<LevelStem> dimension, List<NoiseFeature> noiseFeatures, NoiseOffsets offsets,
            NoiseScaleLadder ladder) {
        this(vanilla, fold, dimension, noiseFeatures, offsets, ladder, NoiseConstants.UNDIVIDED,
                NoiseConstants.UNDIVIDED);
    }

    public static @Nullable FoldedCompileContext of(DensityFunction.CompileContext context) {
        if (!(context instanceof FoldedCompileSource source)) {
            return null;
        }

        FoldedCompileContext folded = source.exotic_worlds$getFoldedCompile();
        if (folded == null) {
            return null;
        }

        GenerationFoldContext.Context scope = GenerationFoldContext.getContext();
        return scope.getXDivisor() == folded.xDivisor && scope.getZDivisor() == folded.zDivisor
                ? folded
                : new FoldedCompileContext(folded.vanilla, folded.fold, folded.dimension, folded.noiseFeatures,
                        folded.offsets, folded.ladder,
                        scope.getXDivisor(), scope.getZDivisor());
    }
}
