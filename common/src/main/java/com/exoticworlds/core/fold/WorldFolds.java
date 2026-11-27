package com.exoticworlds.core.fold;

import java.util.Optional;

import com.exoticworlds.core.fold.cuboid.CuboidFold;
import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.fold.loop.WorldLoopFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CuboidShape;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.mojang.serialization.DataResult;

import net.minecraft.core.Direction;

public final class WorldFolds {
    private static final String REVERSED_LOCAL_INDICES = "its seam reverses the local indices inside a chunk";

    public static final WorldFold NOOP = of(FlatShape.rectangle());

    public static WorldFold of(ShapeModel shape) {
        return of(shape, ShapeOptions.DEFAULT);
    }

    public static WorldFold of(ShapeModel shape, ShapeOptions options) {
        return switch (shape) {
            case FlatShape flat -> flatFold(flat, options);
            case CuboidShape cuboid -> new CuboidFold(cuboid.atlas(), options);
        };
    }

    private static WorldFold flatFold(FlatShape shape, ShapeOptions options) {
        verifyFoldable(shape).getOrThrow(IllegalArgumentException::new);

        return shape.isDecomposedPerAxis()
                ? new WorldLoopFold(shape.bounds(), options)
                : new DeckGroupFold(shape, options);
    }

    public static DataResult<ShapeModel> verifyFoldable(ShapeModel shape) {
        return shape instanceof FlatShape flat ? verifyFoldable(flat).map(foldable -> shape) : DataResult.success(shape);
    }

    public static DataResult<FlatShape> verifyFoldable(FlatShape shape) {
        return shape.isPreservingLocalIndices()
                ? DataResult.success(shape)
                : DataResult.error(() -> refusal(shape, REVERSED_LOCAL_INDICES));
    }

    public static DataResult<ShapeModel> verifyGeneratable(ShapeModel shape) {
        Optional<String> narrow = switch (shape) {
            case FlatShape flat -> narrowAxis(flat);
            case CuboidShape cuboid -> narrowEdge(cuboid);
        };
        return narrow.<DataResult<ShapeModel>>map(reason -> DataResult.error(() -> reason))
                .orElseGet(() -> DataResult.success(shape));
    }

    private static Optional<String> narrowAxis(FlatShape shape) {
        return narrowAxis(shape.bounds().x(), Direction.Axis.X)
                .or(() -> narrowAxis(shape.bounds().z(), Direction.Axis.Z));
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

    private static Optional<String> narrowEdge(CuboidShape shape) {
        return narrowEdge(shape.aChunks(), CuboidShape.A_CHUNKS_KEY)
                .or(() -> narrowEdge(shape.bChunks(), CuboidShape.B_CHUNKS_KEY))
                .or(() -> narrowEdge(shape.cChunks(), CuboidShape.C_CHUNKS_KEY));
    }

    private static Optional<String> narrowEdge(int edgeChunks, String key) {
        if (edgeChunks >= WorldLoopSizes.MIN_WIDTH_CHUNKS) {
            return Optional.empty();
        }

        return Optional.of("A cuboid whose " + key + " edge is " + WorldLoopSizes.describe(edgeChunks)
                + " is under the " + WorldLoopSizes.describe(WorldLoopSizes.MIN_WIDTH_CHUNKS) + " a cuboid edge needs");
    }

    private static String refusal(FlatShape shape, String reason) {
        return "The wrap engine has no fold for " + shape.getIdentification() + " yet: " + reason;
    }

    private WorldFolds() {
    }
}
