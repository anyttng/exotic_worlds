package com.exoticworlds.bridge.v2.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.shape.LoopSpans;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.ShapeSpansAdapter;

import net.minecraft.core.Direction;

public final class SpansBridge {

    public static void requireAscending(int minChunk, int maxChunk) {
        new WorldLoopBounds.AxisBounds.Looped(minChunk, maxChunk);
    }

    public static int normalizedSkew(LoopSpans.@Nullable Span x, LoopSpans.@Nullable Span z, int skewChunks) {
        return FlatShape.latticeTorus(ShapeSpansAdapter.bounds(x, z), skewChunks).skewChunks();
    }

    public static int minChunkWidth() {
        return WorldLoopSizes.MIN_CHUNK_WIDTH;
    }

    public static LoopSpans ofWidth(int chunkWidth) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidth(chunkWidth)));
    }

    public static LoopSpans ofWidths(int xChunkWidth, int zChunkWidth) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidths(xChunkWidth, zChunkWidth)));
    }

    public static LoopSpans ofWidth(Direction.Axis axis, int chunkWidth) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidth(axis, chunkWidth)));
    }

    public static LoopSpans scaledDown(LoopSpans spans, int scale) {
        return ShapeSpansAdapter.loopSpans(ShapeSpansAdapter.flatShape(spans).scaledDown(scale));
    }

    private SpansBridge() {
    }
}
