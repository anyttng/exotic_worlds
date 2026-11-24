package com.exoticworlds.bridge.v2.shape;

import com.exoticworlds.api.v2.shape.DimensionContextV2;
import com.exoticworlds.api.v2.shape.DimensionShapeV2;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.dimension.AbstractDimensionShape;
import com.exoticworlds.shape.dimension.DimensionContext;

final class DimensionShapeView extends AbstractDimensionShape {
    private final DimensionShapeV2 declared;

    DimensionShapeView(DimensionShapeV2 declared) {
        super(declared.getId());
        this.declared = declared;
    }

    @Override
    public FlatShape spans(DimensionContext context) {
        return ShapeSpansAdapter.flatShape(this.declared.spans(toV2(context)));
    }

    @Override
    public boolean canTakePresets() {
        return this.declared.canTakePresets();
    }

    @Override
    public ShapeOptions withPreset(int widthChunks, ShapeOptions options) {
        return ShapeOptionsAdapter.fromV2(this.declared.withPreset(widthChunks, ShapeOptionsAdapter.toV2(options)));
    }

    private static DimensionContextV2 toV2(DimensionContext context) {
        FlatShape overworld = context.overworld();
        return new DimensionContextV2(context.dimension(), ShapeOptionsAdapter.toV2(context.options()),
                overworld == null ? null : ShapeSpansAdapter.loopSpans(overworld), context.overworldScale(),
                context.coordinateScale());
    }
}
