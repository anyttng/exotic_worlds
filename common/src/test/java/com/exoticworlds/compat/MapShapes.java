package com.exoticworlds.compat;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

public final class MapShapes {
    private static final int WIDE_WINDOW_REACH_BLOCKS = 20000;
    private static final int WIDE_WINDOW_LAPS = 3;

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

    public static List<WorldCopies.Window> windows(@Nullable ToroidalShape shape) {
        int width = Math.max(AxisCopies.of(shape, Direction.Axis.X).width(),
                AxisCopies.of(shape, Direction.Axis.Z).width());
        int reach = WIDE_WINDOW_REACH_BLOCKS + WIDE_WINDOW_LAPS * width;
        return List.of(WorldCopies.Window.of(shape, 0, 0, 0, 0),
                WorldCopies.Window.of(shape, -reach, -reach, reach, reach));
    }

    private MapShapes() {
    }
}
