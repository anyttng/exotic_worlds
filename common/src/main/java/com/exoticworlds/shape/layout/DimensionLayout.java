package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;

import net.minecraft.resources.Identifier;

public record DimensionLayout(ShapeKind kind, @Nullable Identifier shape, @Nullable FlatShape spans,
        ShapeOptions options) {
    public DimensionLayout {
        if (kind == ShapeKind.SHAPED ? shape == null : shape != null || spans != null) {
            throw new IllegalArgumentException("A " + kind + " dimension cannot hold the shape " + shape
                    + " and the spans " + spans);
        }

        if (spans != null && spans.mirror() != null) {
            throw new IllegalArgumentException("A dimension layout holds no mirrored shape: " + spans);
        }
    }

    public static DimensionLayout normal(ShapeOptions options) {
        return new DimensionLayout(ShapeKind.NORMAL, null, null, options);
    }

    public static DimensionLayout fromOverworld(ShapeOptions options) {
        return new DimensionLayout(ShapeKind.FROM_OVERWORLD, null, null, options);
    }

    public static DimensionLayout shaped(Identifier shape, @Nullable FlatShape spans, ShapeOptions options) {
        boolean isLooping = spans != null && spans.getIdentification() != FlatShape.Identification.RECTANGLE;
        return new DimensionLayout(ShapeKind.SHAPED, shape, isLooping ? spans : null, options);
    }

    public DimensionLayout withOptions(ShapeOptions chosen) {
        return new DimensionLayout(this.kind, this.shape, this.spans, chosen);
    }
}
