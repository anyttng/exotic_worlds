package com.exoticworlds.bridge.v2.geometry;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

public final class ShapeSpansAdapter {

    public static FlatShape flatShape(LoopSpansV2 spans) {
        return FlatShape.latticeTorus(bounds(spans.x(), spans.z()), spans.skewChunks());
    }

    public static LoopSpansV2 loopSpans(FlatShape shape) {
        if (shape.mirror() != null) {
            throw new IllegalArgumentException("Spans state no mirrored shape: " + shape);
        }

        return new LoopSpansV2(span(shape.bounds().x()), span(shape.bounds().z()), shape.skewChunks());
    }

    public static WorldLoopBounds bounds(LoopSpansV2.@Nullable Span x, LoopSpansV2.@Nullable Span z) {
        return new WorldLoopBounds(axis(x), axis(z));
    }

    private static AxisBounds axis(LoopSpansV2.@Nullable Span span) {
        return span == null ? AxisBounds.Unbounded.INSTANCE : new AxisBounds.Looped(span.minChunk(), span.maxChunk());
    }

    private static LoopSpansV2.@Nullable Span span(AxisBounds axis) {
        return axis instanceof AxisBounds.Looped looped ? new LoopSpansV2.Span(looped.minChunk(), looped.maxChunk()) : null;
    }

    private ShapeSpansAdapter() {
    }
}
