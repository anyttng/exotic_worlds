package com.exoticworlds.engine.noise;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.FoldedRandomState;
import com.exoticworlds.core.fold.WorldFold;
import com.google.common.collect.MapMaker;

import net.minecraft.world.level.levelgen.RandomState;

public final class FoldedCompileContexts {
    // Weak values: a context holds its fold, so a strong value would keep its weak key alive forever.
    private static final Map<WorldFold, FoldedCompileContext> BY_FOLD =
            new MapMaker().weakKeys().weakValues().makeMap();

    public static void register(RandomState state) {
        register(((FoldedRandomState) (Object) state).exotic_worlds$getCompileContext());
    }

    static void register(@Nullable FoldedCompileContext context) {
        if (context != null) {
            BY_FOLD.put(context.fold(), context);
        }
    }

    public static @Nullable FoldedCompileContext get(WorldFold fold) {
        return BY_FOLD.get(fold);
    }

    private FoldedCompileContexts() {
    }
}
