package com.exoticworlds.compat.map;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

public final class MapFolds {
    private static final int WIDE_WINDOW_REACH_BLOCKS = 20000;
    private static final int WIDE_WINDOW_LAPS = 3;

    public static WorldFold torus(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(looped, looped)));
    }

    public static WorldFold cylinder(int minChunk, int maxChunk) {
        return WorldFolds.of(ShapeFixtures.cylinder(
                new WorldLoopBounds(new AxisBounds.Looped(minChunk, maxChunk), AxisBounds.Unbounded.INSTANCE)));
    }

    public static WorldFold latticeTorus(int minChunk, int maxChunk, int skewChunks) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return 
                new DeckGroupFold(FlatShape.latticeTorus(new WorldLoopBounds(looped, looped), skewChunks),
                        ShapeOptions.DEFAULT);
    }

    public static List<WorldCopies.Window> windows(@Nullable WorldFold shape) {
        int width = Math.max(AxisCopies.of(shape, Direction.Axis.X).width(),
                AxisCopies.of(shape, Direction.Axis.Z).width());
        int reach = WIDE_WINDOW_REACH_BLOCKS + WIDE_WINDOW_LAPS * width;
        return List.of(WorldCopies.Window.of(shape, 0, 0, 0, 0),
                WorldCopies.Window.of(shape, -reach, -reach, reach, reach));
    }

    private MapFolds() {
    }
}
