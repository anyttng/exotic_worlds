package com.exoticworlds.shape.cylinder;

import java.util.Optional;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.LoopLogic;

import net.minecraft.core.Direction;

public final class CylinderLogic extends LoopLogic {

    CylinderLogic() {
    }

    @Override
    public FlatShape spans(DimensionContext context) {
        CylinderSize size = context.options().get(CylinderParameters.SIZE);
        return FlatShape.unskewed(WorldLoopBounds.ofWidth(size.axis(), size.widthChunks()));
    }

    @Override
    protected ShapeOptions withWidth(int widthChunks, ShapeOptions options) {
        return options.with(CylinderParameters.SIZE,
                new CylinderSize(options.get(CylinderParameters.SIZE).axis(), widthChunks));
    }

    @Override
    public Optional<ShapeOptions> optionsFor(FlatShape spans) {
        boolean isXLooped = spans.bounds().isLooped(Direction.Axis.X);
        if (isXLooped == spans.bounds().isLooped(Direction.Axis.Z)) {
            return Optional.empty();
        }

        Direction.Axis axis = isXLooped ? Direction.Axis.X : Direction.Axis.Z;
        return Optional.of(ShapeOptions.DEFAULT.with(CylinderParameters.SIZE,
                new CylinderSize(axis, spans.bounds().getWidthChunks(axis))));
    }
}
