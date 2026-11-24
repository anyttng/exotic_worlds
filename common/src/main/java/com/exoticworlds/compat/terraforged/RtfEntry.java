package com.exoticworlds.compat.terraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.engine.noise.GenerationFoldContext;

public final class RtfEntry {
    public static @Nullable WorldFold getGenerationFold() {
        WorldFold fold = getLapFold();
        return fold.isWrapped() ? fold : null;
    }

    public static WorldFold getLapFold() {
        return RtfLap.isActive() ? GenerationFoldContext.getContext().getFold() : WorldFolds.NOOP;
    }

    public static int foldX(WorldFold fold, int x, int z) {
        return fold.getBlockLattice().foldX(x, z);
    }

    public static float foldX(WorldFold fold, float x, float z) {
        return (float) fold.getBlockLattice().foldX((double) x, (double) z);
    }

    public static int foldZ(WorldFold fold, int z) {
        return fold.getBlockLattice().foldZ(z);
    }

    public static float foldZ(WorldFold fold, float z) {
        return (float) fold.getBlockLattice().foldZ((double) z);
    }

    private RtfEntry() {
    }
}
