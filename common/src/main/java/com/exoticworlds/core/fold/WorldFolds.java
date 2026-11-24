package com.exoticworlds.core.fold;

import java.util.Optional;

import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.fold.loop.WorldLoopFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.mojang.serialization.DataResult;

import net.minecraft.core.Direction;

public final class WorldFolds {
    private static final String REVERSED_LOCAL_INDICES = "its seam reverses the local indices inside a chunk";

    public static final WorldFold NOOP = of(FlatShape.rectangle());

    public static WorldFold of(FlatShape shape) {
        return of(shape, ShapeOptions.DEFAULT);
    }

    public static WorldFold of(FlatShape shape, ShapeOptions options) {
        verifyFoldable(shape).getOrThrow(IllegalArgumentException::new);

        return shape.isDecomposedPerAxis()
                ? new WorldLoopFold(shape.bounds(), options)
                : new DeckGroupFold(shape, options);
    }

    public static DataResult<FlatShape> verifyFoldable(FlatShape shape) {
        return shape.isPreservingLocalIndices()
                ? DataResult.success(shape)
                : DataResult.error(() -> refusal(shape, REVERSED_LOCAL_INDICES));
    }

    public static DataResult<FlatShape> verifyGeneratable(FlatShape shape) {
        Optional<String> narrow = narrowAxis(shape.bounds().x(), Direction.Axis.X)
                .or(() -> narrowAxis(shape.bounds().z(), Direction.Axis.Z));
        return narrow.<DataResult<FlatShape>>map(reason -> DataResult.error(() -> reason))
                .orElseGet(() -> DataResult.success(shape));
    }

    private static Optional<String> narrowAxis(AxisBounds bounds, Direction.Axis axis) {
        if (!(bounds instanceof AxisBounds.Looped looped)
                || looped.getWidthChunks() >= WorldLoopSizes.MIN_WIDTH_CHUNKS) {
            return Optional.empty();
        }

        return Optional.of("A world looping " + WorldLoopSizes.describe(looped.getWidthChunks()) + " on its "
                + axis.getName() + " axis is under the " + WorldLoopSizes.describe(WorldLoopSizes.MIN_WIDTH_CHUNKS)
                + " chunk generation needs, and would crash while generating");
    }

    private static String refusal(FlatShape shape, String reason) {
        return "The wrap engine has no fold for " + shape.getIdentification() + " yet: " + reason;
    }

    private WorldFolds() {
    }
}
