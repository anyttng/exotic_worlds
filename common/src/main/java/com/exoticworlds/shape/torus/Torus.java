package com.exoticworlds.shape.torus;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.dimension.AbstractDimensionShape;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;

public final class Torus extends AbstractDimensionShape {
    private static final int SIZE_POSITION = 0;

    public static final ShapeOption<Integer> X_WIDTH =
            new ShapeOption<>("torus_x_width", SIZE_POSITION, Codec.INT, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS);
    public static final ShapeOption<Integer> Z_WIDTH =
            new ShapeOption<>("torus_z_width", SIZE_POSITION, Codec.INT, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS);
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
    public ShapeOptions withPreset(int widthChunks, ShapeOptions options) {
        return options.with(X_WIDTH, widthChunks).with(Z_WIDTH, widthChunks).with(SHIFT, FlatShape.NO_SKEW);
    }

    @Override
    public ShapeOptions optionsFor(FlatShape spans) {
        int xWidthChunks = spans.bounds().getWidthChunks(Direction.Axis.X);
        return ShapeOptions.DEFAULT
                .with(X_WIDTH, xWidthChunks)
                .with(Z_WIDTH, spans.bounds().getWidthChunks(Direction.Axis.Z))
                .with(SHIFT, Math.floorMod(spans.skewChunks(), xWidthChunks));
    }
}
