package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.resources.Identifier;

public record DimensionLayout(ShapeKind kind, @Nullable Identifier shape, @Nullable FlatShape spans,
        ShapeOptions options) {
    public static final ShapeOptions DEFAULT_OPTIONS =
            ShapeOptions.DEFAULT.with(CompactBiomes.FEATURE.getOption(), ClimateScale.OFF);

    public DimensionLayout {
        boolean isLooped = kind == ShapeKind.TORUS || kind == ShapeKind.CYLINDER;
        if (isLooped ? kind != ShapeKind.of(spans) : spans != null) {
            throw new IllegalArgumentException("A " + kind + " dimension cannot hold the spans " + spans);
        }

        if (spans != null && spans.mirror() != null) {
            throw new IllegalArgumentException("A dimension layout holds no mirrored shape: " + spans);
        }

        if (shape != null && kind == ShapeKind.FROM_OVERWORLD) {
            throw new IllegalArgumentException("A dimension from the Overworld names no shape of its own: " + shape);
        }
    }

    public static DimensionLayout normal(ShapeOptions options) {
        return new DimensionLayout(ShapeKind.NORMAL, null, null, options);
    }

    public static DimensionLayout fromOverworld(ShapeOptions options) {
        return new DimensionLayout(ShapeKind.FROM_OVERWORLD, null, null, options);
    }

    public static DimensionLayout looped(@Nullable FlatShape spans, ShapeOptions options) {
        ShapeKind kind = ShapeKind.of(spans);
        return kind == ShapeKind.NORMAL ? normal(options) : new DimensionLayout(kind, null, spans, options);
    }

    public static DimensionLayout declared(Identifier shape, @Nullable FlatShape spans, ShapeOptions options) {
        ShapeKind kind = ShapeKind.of(spans);
        return new DimensionLayout(kind, shape, kind == ShapeKind.NORMAL ? null : spans, options);
    }

    public DimensionLayout withOptions(ShapeOptions chosen) {
        return new DimensionLayout(this.kind, this.shape, this.spans, chosen);
    }
}
