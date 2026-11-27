package com.exoticworlds.shape.torus;

import java.util.Optional;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.LoopLogic;

import net.minecraft.core.Direction;

public final class TorusLogic extends LoopLogic {

    TorusLogic() {
    }

    @Override
    public FlatShape spans(DimensionContext context) {
        TorusSize size = context.options().get(TorusParameters.SIZE);
        return FlatShape.latticeTorus(WorldLoopBounds.ofWidths(size.xWidthChunks(), size.zWidthChunks()),
                size.shiftChunks());
    }

    @Override
    protected ShapeOptions withWidth(int widthChunks, ShapeOptions options) {
        return options.with(TorusParameters.SIZE, TorusSize.square(widthChunks));
    }

    @Override
    public Optional<ShapeOptions> optionsFor(ShapeModel shape) {
        if (!(shape instanceof FlatShape spans)
                || !spans.bounds().isLooped(Direction.Axis.X) || !spans.bounds().isLooped(Direction.Axis.Z)) {
            return Optional.empty();
        }

        int xWidthChunks = spans.bounds().getWidthChunks(Direction.Axis.X);
        return Optional.of(ShapeOptions.DEFAULT.with(TorusParameters.SIZE, new TorusSize(xWidthChunks,
                spans.bounds().getWidthChunks(Direction.Axis.Z), Math.floorMod(spans.skewChunks(), xWidthChunks))));
    }
}
