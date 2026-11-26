package com.exoticworlds.bridge.v1.shape;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopSizes;

import net.minecraft.core.Direction;

public final class SpansBridge {
    private static final String AXIS = "Axis ";
    private static final String DOES_NOT_LOOP = " does not loop, check loops(axis) first";

    public record Spans(WorldLoopBounds bounds, int skewChunks) {
        public Spans {
            skewChunks = FlatShape.latticeTorus(bounds, skewChunks).skewChunks();
        }
    }

    public static Spans none() {
        return new Spans(WorldLoopBounds.UNBOUNDED, FlatShape.NO_SKEW);
    }

    public static int getMinWidthChunks() {
        return WorldLoopSizes.MIN_WIDTH_CHUNKS;
    }

    public static Spans ofWidth(int widthChunks) {
        return new Spans(WorldLoopBounds.ofWidth(widthChunks), FlatShape.NO_SKEW);
    }

    public static Spans ofWidths(int xWidthChunks, int zWidthChunks) {
        return new Spans(WorldLoopBounds.ofWidths(xWidthChunks, zWidthChunks), FlatShape.NO_SKEW);
    }

    public static Spans ofWidth(Direction.Axis axis, int widthChunks) {
        return new Spans(WorldLoopBounds.ofWidth(axis, widthChunks), FlatShape.NO_SKEW);
    }

    public static Spans and(Spans spans, Direction.Axis axis, int minChunk, int maxChunk) {
        return new Spans(spans.bounds().with(axis, new AxisBounds.Looped(minChunk, maxChunk)), spans.skewChunks());
    }

    public static Spans withSkew(Spans spans, int skewChunks) {
        return new Spans(spans.bounds(), skewChunks);
    }

    public static boolean loops(Spans spans, Direction.Axis axis) {
        return axis != Direction.Axis.Y && spans.bounds().isLooped(axis);
    }

    public static int minChunk(Spans spans, Direction.Axis axis) {
        return looping(spans, axis).minChunk();
    }

    public static int maxChunk(Spans spans, Direction.Axis axis) {
        return looping(spans, axis).maxChunk();
    }

    public static int getWidthChunks(Spans spans, Direction.Axis axis) {
        return looping(spans, axis).getWidthChunks();
    }

    public static boolean isSquare(Spans spans) {
        return spans.bounds().isSquare();
    }

    public static Spans scaledDown(Spans spans, int scale) {
        FlatShape scaled = FlatShape.latticeTorus(spans.bounds(), spans.skewChunks()).scaledDown(scale);
        return new Spans(scaled.bounds(), scaled.skewChunks());
    }

    public static String text(Spans spans) {
        return "LoopSpans[x=" + spans.bounds().x().getSpanText() + ", z=" + spans.bounds().z().getSpanText()
                + ", skew=" + spans.skewChunks() + "]";
    }

    private static AxisBounds.Looped looping(Spans spans, Direction.Axis axis) {
        if (spans.bounds().getAxis(axis) instanceof AxisBounds.Looped looped) {
            return looped;
        }

        throw new IllegalArgumentException(AXIS + axis + DOES_NOT_LOOP);
    }

    private SpansBridge() {
    }
}
