package com.exoticworlds.core;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.world.CarriedShape;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.MapMaker;

import net.minecraft.world.level.chunk.ChunkGenerator;

public final class FoldGenerators {
    private static final Map<WorldFold, ChunkGenerator> BY_FOLD = new MapMaker().weakKeys().weakValues().makeMap();

    public static void fileFold(ChunkGenerator generator, @Nullable CarriedShape carried) {
        if (carried != null) {
            BY_FOLD.put(carried.getFold(), generator);
        }
    }

    public static @Nullable ChunkGenerator getGenerator(WorldFold fold) {
        return BY_FOLD.get(fold);
    }

    private FoldGenerators() {
    }
}
