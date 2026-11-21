package com.exoticworlds.core;

import java.util.Optional;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.mojang.serialization.DataResult;

import net.minecraft.core.Direction;

public final class WorldFolds {
    private static final String REVERSED_LOCAL_INDICES = "its seam reverses the local indices inside a chunk";

    public static final WorldFold NOOP = of(FlatShape.rectangle());

    public static WorldFold of(ShapeModel shape) {
        return of(shape, GenerationOptions.DEFAULT);
    }

    public static WorldFold of(ShapeModel shape, GenerationOptions generationOptions) {
        return switch (shape) {
            case FlatShape flat -> flatFold(flat, generationOptions);
            case CuboidShape cuboid -> new CuboidFold(cuboid.atlas(), generationOptions);
        };
    }

    private static WorldFold flatFold(FlatShape shape, GenerationOptions generationOptions) {
        verifyFoldable(shape).getOrThrow(IllegalArgumentException::new);

        return shape.decomposesPerAxis()
                ? new WorldLoopFold(shape.bounds(), generationOptions)
                : new DeckGroupFold(shape, generationOptions);
    }

    public static DataResult<ShapeModel> verifyFoldable(ShapeModel shape) {
        return shape instanceof FlatShape flat ? verifyFoldable(flat).map(foldable -> shape) : DataResult.success(shape);
    }

    public static DataResult<FlatShape> verifyFoldable(FlatShape shape) {
        return shape.preservesLocalIndices()
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
                || looped.chunkWidth() >= WorldLoopSizes.MIN_CHUNK_WIDTH) {
            return Optional.empty();
        }

        return Optional.of("A world looping " + WorldLoopSizes.describe(looped.chunkWidth()) + " on its "
                + axis.getName() + " axis is under the " + WorldLoopSizes.describe(WorldLoopSizes.MIN_CHUNK_WIDTH)
                + " chunk generation needs, and would crash while generating");
    }

    private static Optional<String> narrowEdge(CuboidShape shape) {
        return narrowEdge(shape.aChunks(), CuboidShape.A_CHUNKS_KEY)
                .or(() -> narrowEdge(shape.bChunks(), CuboidShape.B_CHUNKS_KEY))
                .or(() -> narrowEdge(shape.cChunks(), CuboidShape.C_CHUNKS_KEY));
    }

    private static Optional<String> narrowEdge(int chunks, String key) {
        if (chunks >= WorldLoopSizes.MIN_CHUNK_WIDTH) {
            return Optional.empty();
        }

        return Optional.of("A cuboid whose " + key + " edge is " + WorldLoopSizes.describe(chunks)
                + " is under the " + WorldLoopSizes.describe(WorldLoopSizes.MIN_CHUNK_WIDTH) + " a cuboid edge needs");
    }

    private static String refusal(FlatShape shape, String reason) {
        return "The wrap engine has no fold for " + shape.identification() + " yet: " + reason;
    }

    private WorldFolds() {
    }
}
