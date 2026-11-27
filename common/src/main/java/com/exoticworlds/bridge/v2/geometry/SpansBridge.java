package com.exoticworlds.bridge.v2.geometry;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;

import net.minecraft.core.Direction;

public final class SpansBridge {

    public static void requireAscending(int minChunk, int maxChunk) {
        new WorldLoopBounds.AxisBounds.Looped(minChunk, maxChunk);
    }

    public static int normalizedSkewChunks(LoopSpansV2.@Nullable Span x, LoopSpansV2.@Nullable Span z, int skewChunks) {
        return FlatShape.latticeTorus(ShapeSpansAdapter.bounds(x, z), skewChunks).skewChunks();
    }

    public static int getMinWidthChunks() {
        return WorldLoopSizes.MIN_WIDTH_CHUNKS;
    }

    public static LoopSpansV2 ofWidth(int widthChunks) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidth(widthChunks)));
    }

    public static LoopSpansV2 ofWidths(int xWidthChunks, int zWidthChunks) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidths(xWidthChunks, zWidthChunks)));
    }

    public static LoopSpansV2 ofWidth(Direction.Axis axis, int widthChunks) {
        return ShapeSpansAdapter.loopSpans(FlatShape.unskewed(WorldLoopBounds.ofWidth(axis, widthChunks)));
    }

    public static LoopSpansV2 scaledDown(LoopSpansV2 spans, int scale) {
        return ShapeSpansAdapter.loopSpans(ShapeSpansAdapter.flatShape(spans).scaledDown(scale));
    }

    private SpansBridge() {
    }
}
