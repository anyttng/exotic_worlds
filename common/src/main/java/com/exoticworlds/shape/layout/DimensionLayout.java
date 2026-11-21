package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.CuboidShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.resources.Identifier;

public record DimensionLayout(ShapeKind kind, @Nullable Identifier shape, @Nullable LoopSpans spans,
        @Nullable CuboidShape cuboid, GenerationOptions options) {
    public static final GenerationOptions DEFAULT_OPTIONS =
            GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF);

    public DimensionLayout {
        boolean loops = kind == ShapeKind.TORUS || kind == ShapeKind.CYLINDER;
        if (loops ? kind != ShapeKind.of(spans) : spans != null) {
            throw new IllegalArgumentException("A " + kind + " dimension cannot hold the spans " + spans);
        }

        if (kind == ShapeKind.CUBOID ? cuboid == null : cuboid != null) {
            throw new IllegalArgumentException("A " + kind + " dimension cannot hold the cuboid " + cuboid);
        }

        if (cuboid != null) {
            WorldFolds.verifyGeneratable(cuboid).getOrThrow(IllegalArgumentException::new);
        }

        if (shape != null && kind == ShapeKind.FROM_OVERWORLD) {
            throw new IllegalArgumentException("A dimension from the Overworld names no shape of its own: " + shape);
        }
    }

    public static DimensionLayout normal(GenerationOptions options) {
        return new DimensionLayout(ShapeKind.NORMAL, null, null, null, options);
    }

    public static DimensionLayout fromOverworld(GenerationOptions options) {
        return new DimensionLayout(ShapeKind.FROM_OVERWORLD, null, null, null, options);
    }

    public static DimensionLayout looped(@Nullable LoopSpans spans, GenerationOptions options) {
        ShapeKind kind = ShapeKind.of(spans);
        return kind == ShapeKind.NORMAL ? normal(options) : new DimensionLayout(kind, null, spans, null, options);
    }

    public static DimensionLayout declared(Identifier shape, @Nullable LoopSpans spans, GenerationOptions options) {
        ShapeKind kind = ShapeKind.of(spans);
        return new DimensionLayout(kind, shape, kind == ShapeKind.NORMAL ? null : spans, null, options);
    }

    public static DimensionLayout cuboid(CuboidShape cuboid) {
        return new DimensionLayout(ShapeKind.CUBOID, null, null, cuboid, DEFAULT_OPTIONS);
    }

    public DimensionLayout withOptions(GenerationOptions chosen) {
        return new DimensionLayout(this.kind, this.shape, this.spans, this.cuboid, chosen);
    }
}
