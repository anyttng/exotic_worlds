package com.exoticworlds.compat.map;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

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
        int widthBlocks = Math.max(AxisCopies.of(shape, Direction.Axis.X).getWidth(),
                AxisCopies.of(shape, Direction.Axis.Z).getWidth());
        int reachBlocks = WIDE_WINDOW_REACH_BLOCKS + WIDE_WINDOW_LAPS * widthBlocks;
        return List.of(WorldCopies.Window.of(shape, 0, 0, 0, 0),
                WorldCopies.Window.of(shape, -reachBlocks, -reachBlocks, reachBlocks, reachBlocks));
    }

    private MapFolds() {
    }
}
