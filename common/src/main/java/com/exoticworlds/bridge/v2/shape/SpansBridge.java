package com.exoticworlds.bridge.v2.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.shape.LoopSpansV2;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.core.Direction;

public final class SpansBridge {

    public static void requireAscending(int minChunk, int maxChunk) {
        new WorldLoopBounds.AxisBounds.Looped(minChunk, maxChunk);
    }

    public static int normalizedSkew(LoopSpansV2.@Nullable Span x, LoopSpansV2.@Nullable Span z, int skewChunks) {
        return FlatShape.latticeTorus(ShapeSpansAdapter.bounds(x, z), skewChunks).skewChunks();
    }

    public static int minChunkWidth() {
        return WorldLoopSizes.MIN_CHUNK_WIDTH;
    }

    public static LoopSpansV2 ofWidth(int chunkWidth) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidth(chunkWidth)));
    }

    public static LoopSpansV2 ofWidths(int xChunkWidth, int zChunkWidth) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidths(xChunkWidth, zChunkWidth)));
    }

    public static LoopSpansV2 ofWidth(Direction.Axis axis, int chunkWidth) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidth(axis, chunkWidth)));
    }

    public static LoopSpansV2 scaledDown(LoopSpansV2 spans, int scale) {
        return ShapeSpansAdapter.loopSpans(ShapeSpansAdapter.flatShape(spans).scaledDown(scale));
    }

    private SpansBridge() {
    }
}
