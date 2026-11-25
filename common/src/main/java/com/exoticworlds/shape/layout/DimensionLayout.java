package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;

import net.minecraft.resources.Identifier;

public record DimensionLayout(DimensionMode mode, @Nullable Identifier shape, @Nullable FlatShape spans,
        ShapeOptions options) {
    public DimensionLayout {
        if (mode == DimensionMode.SHAPED ? shape == null : shape != null || spans != null) {
            throw new IllegalArgumentException("A " + mode + " dimension cannot hold the shape " + shape
                    + " and the spans " + spans);
        }

        if (spans != null && spans.mirror() != null) {
            throw new IllegalArgumentException("A dimension layout holds no mirrored shape: " + spans);
        }
    }

    public static DimensionLayout normal(ShapeOptions options) {
        return new DimensionLayout(DimensionMode.NORMAL, null, null, options);
    }

    public static DimensionLayout fromOverworld(ShapeOptions options) {
        return new DimensionLayout(DimensionMode.FROM_OVERWORLD, null, null, options);
    }

    public static DimensionLayout shaped(Identifier shape, @Nullable FlatShape spans, ShapeOptions options) {
        boolean isLooping = spans != null && spans.getIdentification() != FlatShape.Identification.RECTANGLE;
        return new DimensionLayout(DimensionMode.SHAPED, shape, isLooping ? spans : null, options);
    }

    public DimensionLayout withOptions(ShapeOptions chosen) {
        return new DimensionLayout(this.mode, this.shape, this.spans, chosen);
    }
}
