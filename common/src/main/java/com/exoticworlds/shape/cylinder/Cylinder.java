package com.exoticworlds.shape.cylinder;

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

public final class Cylinder extends AbstractDimensionShape {
    private static final int SIZE_POSITION = 0;

    public static final ShapeOption<Direction.Axis> AXIS =
            new ShapeOption<>("cylinder_axis", SIZE_POSITION, Direction.Axis.CODEC, Direction.Axis.X);
    public static final ShapeOption<Integer> WIDTH =
            new ShapeOption<>("cylinder_width", SIZE_POSITION, Codec.INT, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS);

    public static final Cylinder SHAPE = new Cylinder();

    private Cylinder() {
        super(DeclaredShapes.CYLINDER);
    }

    @Override
    public FlatShape spans(DimensionContext context) {
        ShapeOptions options = context.options();
        return FlatShape.unskewed(WorldLoopBounds.ofWidth(options.get(AXIS), options.get(WIDTH)));
    }

    @Override
    public ShapeOptions withPreset(int widthChunks, ShapeOptions options) {
        return options.with(WIDTH, widthChunks);
    }

    @Override
    public ShapeOptions optionsFor(FlatShape spans) {
        Direction.Axis axis = spans.bounds().isLooped(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
        return ShapeOptions.DEFAULT.with(AXIS, axis).with(WIDTH, spans.bounds().getWidthChunks(axis));
    }
}
