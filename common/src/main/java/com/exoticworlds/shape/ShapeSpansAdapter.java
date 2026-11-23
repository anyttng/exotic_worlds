package com.exoticworlds.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.shape.LoopSpans;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

public final class ShapeSpansAdapter {

    public static FlatShape flatShape(LoopSpans spans) {
        return FlatShape.latticeTorus(bounds(spans.x(), spans.z()), spans.skewChunks());
    }

    public static LoopSpans loopSpans(FlatShape shape) {
        if (shape.mirror() != null) {
            throw new IllegalArgumentException("Spans state no mirrored shape: " + shape);
        }

        return new LoopSpans(span(shape.bounds().x()), span(shape.bounds().z()), shape.skewChunks());
    }

    public static WorldLoopBounds bounds(LoopSpans.@Nullable Span x, LoopSpans.@Nullable Span z) {
        return new WorldLoopBounds(axis(x), axis(z));
    }

    private static AxisBounds axis(LoopSpans.@Nullable Span span) {
        return span == null ? AxisBounds.Unbounded.INSTANCE : new AxisBounds.Looped(span.minChunk(), span.maxChunk());
    }

    private static LoopSpans.@Nullable Span span(AxisBounds axis) {
        return axis instanceof AxisBounds.Looped looped ? new LoopSpans.Span(looped.minChunk(), looped.maxChunk()) : null;
    }

    private ShapeSpansAdapter() {
    }
}
