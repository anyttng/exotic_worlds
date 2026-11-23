package com.exoticworlds.bridge.v2.shape;

import com.exoticworlds.api.v2.shape.DimensionContextV2;
import com.exoticworlds.api.v2.shape.DimensionShapeV2;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;

import net.minecraft.resources.Identifier;

record DimensionShapeView(DimensionShapeV2 declared) implements DimensionShape {

    @Override
    public Identifier id() {
        return this.declared.id();
    }

    @Override
    public FlatShape spans(DimensionContext context) {
        return ShapeSpansAdapter.flatShape(this.declared.spans(contextV2(context)));
    }

    @Override
    public boolean takesPresets() {
        return this.declared.takesPresets();
    }

    @Override
    public ShapeOptions withPreset(int chunkWidth, ShapeOptions options) {
        return ShapeOptionsAdapter.fromV2(this.declared.withPreset(chunkWidth, ShapeOptionsAdapter.toV2(options)));
    }

    private static DimensionContextV2 contextV2(DimensionContext context) {
        FlatShape overworld = context.overworld();
        return new DimensionContextV2(context.dimension(), ShapeOptionsAdapter.toV2(context.options()),
                overworld == null ? null : ShapeSpansAdapter.loopSpans(overworld), context.overworldScale(),
                context.coordinateScale());
    }
}
