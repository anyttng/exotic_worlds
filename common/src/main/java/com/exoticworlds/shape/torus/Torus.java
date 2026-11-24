package com.exoticworlds.shape.torus;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeOption;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.dimension.AbstractDimensionShape;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;

public final class Torus extends AbstractDimensionShape {
    private static final int SIZE_POSITION = 0;

    public static final ShapeOption<Integer> X_WIDTH =
            new ShapeOption<>("torus_x_width", SIZE_POSITION, Codec.INT, WorldLoopSizes.DEFAULT_CHUNK_WIDTH);
    public static final ShapeOption<Integer> Z_WIDTH =
            new ShapeOption<>("torus_z_width", SIZE_POSITION, Codec.INT, WorldLoopSizes.DEFAULT_CHUNK_WIDTH);
    public static final ShapeOption<Integer> SHIFT =
            new ShapeOption<>("torus_shift", SIZE_POSITION, Codec.INT, FlatShape.NO_SKEW);

    public static final Torus SHAPE = new Torus();

    private Torus() {
        super(DeclaredShapes.TORUS);
    }

    @Override
    public FlatShape spans(DimensionContext context) {
        ShapeOptions options = context.options();
        return FlatShape.latticeTorus(WorldLoopBounds.ofWidths(options.get(X_WIDTH), options.get(Z_WIDTH)),
                options.get(SHIFT));
    }

    @Override
    public ShapeOptions withPreset(int chunkWidth, ShapeOptions options) {
        return options.with(X_WIDTH, chunkWidth).with(Z_WIDTH, chunkWidth).with(SHIFT, FlatShape.NO_SKEW);
    }

    @Override
    public ShapeOptions optionsFor(FlatShape spans) {
        int xChunkWidth = spans.bounds().chunkWidth(Direction.Axis.X);
        return ShapeOptions.DEFAULT
                .with(X_WIDTH, xChunkWidth)
                .with(Z_WIDTH, spans.bounds().chunkWidth(Direction.Axis.Z))
                .with(SHIFT, Math.floorMod(spans.skewChunks(), xChunkWidth));
    }
}
