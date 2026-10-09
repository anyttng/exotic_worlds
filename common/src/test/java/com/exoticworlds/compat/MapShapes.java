package com.exoticworlds.compat;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

public final class MapShapes {
    public static ToroidalShape torus(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return TestShapes.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(looped, looped))));
    }

    public static ToroidalShape cylinder(int minChunk, int maxChunk) {
        return TestShapes.of(WorldFolds.of(FlatShape.cylinder(
                new WorldLoopBounds(new AxisBounds.Looped(minChunk, maxChunk), AxisBounds.Unbounded.INSTANCE))));
    }

    public static ToroidalShape latticeTorus(int minChunk, int maxChunk, int skewChunks) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return TestShapes.of(new DeckGroupFold(FlatShape.latticeTorus(new WorldLoopBounds(looped, looped), skewChunks)));
    }

    private MapShapes() {
    }
}
