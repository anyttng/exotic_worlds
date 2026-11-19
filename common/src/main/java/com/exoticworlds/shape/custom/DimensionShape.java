package com.exoticworlds.shape.custom;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;

public record DimensionShape(@Nullable LoopSpans spans, GenerationOptions options, boolean fromOverworld) {

    public DimensionShape {
        if (fromOverworld && spans != null) {
            throw new IllegalArgumentException("A dimension from the Overworld takes its spans from it, got " + spans);
        }
    }

    public DimensionShape(@Nullable LoopSpans spans, GenerationOptions options) {
        this(spans, options, false);
    }

    public static DimensionShape fromOverworld(GenerationOptions options) {
        return new DimensionShape(null, options, true);
    }

    public StemKind kind() {
        if (this.fromOverworld) {
            throw new IllegalStateException("A dimension from the Overworld has the kind of its resolved spans");
        }

        return StemKind.of(this.spans);
    }

    public DimensionShape withKind(StemKind kind, CustomStem dimension) {
        return new DimensionShape(kind.respan(this.spans, dimension.defaultChunkWidth()), this.options);
    }
}
