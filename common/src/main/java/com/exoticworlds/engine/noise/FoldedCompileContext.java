package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.accessors.FoldedCompileSource;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

public record FoldedCompileContext(DensityFunction.CompileContext vanilla, WorldFold fold, CoastLiftCache coastLift,
        NoiseScaleLadder ladder, double xDivisor, double zDivisor, BoxDivisors boxDivisors) {
    public FoldedCompileContext(DensityFunction.CompileContext vanilla, WorldFold fold, CoastLiftCache coastLift,
            NoiseScaleLadder ladder) {
        this(vanilla, fold, coastLift, ladder, NoiseConstants.UNDIVIDED, NoiseConstants.UNDIVIDED,
                BoxDivisors.UNDIVIDED);
    }

    public static @Nullable FoldedCompileContext of(DensityFunction.CompileContext context) {
        if (!(context instanceof FoldedCompileSource source)) {
            return null;
        }

        FoldedCompileContext folded = source.exotic_worlds$foldedCompile();
        if (folded == null) {
            return null;
        }

        GenerationFoldContext.Context scope = GenerationFoldContext.context();
        return scope.xDivisor() == folded.xDivisor && scope.zDivisor() == folded.zDivisor
                && scope.boxDivisors().equals(folded.boxDivisors)
                ? folded
                : new FoldedCompileContext(folded.vanilla, folded.fold, folded.coastLift, folded.ladder,
                        scope.xDivisor(), scope.zDivisor(), scope.boxDivisors());
    }
}
