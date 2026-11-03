package com.exoticworlds.shape.custom;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;

public record DimensionShape(@Nullable LoopSpans spans, GenerationOptions options) {

    public DimensionShape {
        if (StemKind.of(spans) == null) {
            throw new IllegalArgumentException("A custom dimension is a torus or a cylinder, unskewed, got " + spans);
        }
    }

    public StemKind kind() {
        return Objects.requireNonNull(StemKind.of(this.spans), "A stem kind was checked in the constructor");
    }

    public DimensionShape withKind(StemKind kind, CustomStem dimension) {
        return new DimensionShape(kind.respan(this.spans, dimension.defaultChunkWidth()), this.options);
    }
}
