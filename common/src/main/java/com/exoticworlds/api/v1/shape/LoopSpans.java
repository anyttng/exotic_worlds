package com.exoticworlds.api.v1.shape;

import com.exoticworlds.bridge.v1.shape.SpansBridge;

import net.minecraft.core.Direction;

/**
 * What a shape declares about one dimension: which horizontal axes loop, over what span of chunks, and — where both
 * loop — how far crossing the Z seam shifts X. This is the whole of the geometry a shape states — the fold the engine
 * then runs is derived from it, and none of that engine is reachable from here.
 *
 * <p>Spans are half-open in chunks: {@code minChunk} is the first chunk inside the world, {@code maxChunk} the first
 * one past it. An axis with no span does not loop and stretches to the vanilla world border.
 * {@link Direction.Axis#Y} never loops and is refused by every member here.</p>
 *
 * <p>Which flat surface the world ends up being is read off the spans, not declared: both axes give a torus — a
 * lattice torus where it is skewed — one gives a cylinder, neither gives an ordinary world. Immutable; every wither
 * hands back a new instance.</p>
 */
public final class LoopSpans {
    /** Neither axis loops — an ordinary world. */
    public static final LoopSpans NONE = new LoopSpans(SpansBridge.none());

    private final SpansBridge.Spans spans;

    private LoopSpans(SpansBridge.Spans spans) {
        this.spans = spans;
    }

    /**
     * The narrowest a looping axis may be in any dimension, in chunks. {@link ShapeDimensions#withSpans} refuses spans
     * under it, so scale a nether down only as far as this allows.
     */
    public static int minChunkWidth() {
        return SpansBridge.getMinWidthChunks();
    }

    /** Both axes looping over {@code chunkWidth} chunks, centered on the origin. */
    public static LoopSpans ofWidth(int chunkWidth) {
        return new LoopSpans(SpansBridge.ofWidth(chunkWidth));
    }

    /** X looping over {@code xChunkWidth} chunks and Z over {@code zChunkWidth}, both centered on the origin. */
    public static LoopSpans ofWidths(int xChunkWidth, int zChunkWidth) {
        return new LoopSpans(SpansBridge.ofWidths(xChunkWidth, zChunkWidth));
    }

    /** One axis looping over {@code chunkWidth} chunks, centered on the origin; the other stays unbounded. */
    public static LoopSpans ofWidth(Direction.Axis axis, int chunkWidth) {
        return new LoopSpans(SpansBridge.ofWidth(axis, chunkWidth));
    }

    /** One axis looping over the half-open chunk span {@code [minChunk, maxChunk)}; the other stays unbounded. */
    public static LoopSpans of(Direction.Axis axis, int minChunk, int maxChunk) {
        return NONE.and(axis, minChunk, maxChunk);
    }

    /** These spans with {@code axis} looping over {@code [minChunk, maxChunk)} as well. */
    public LoopSpans and(Direction.Axis axis, int minChunk, int maxChunk) {
        return new LoopSpans(SpansBridge.and(this.spans, axis, minChunk, maxChunk));
    }

    /**
     * These spans with crossing the Z seam also shifting X by {@code skewChunks} chunks — a lattice torus. Any whole
     * number of X widths may be added to the skew without changing the world; {@link #skewChunks()} answers it
     * brought into {@code (-xWidth/2, xWidth/2]}, and a skew of {@code 0} is the plain torus.
     *
     * @throws IllegalArgumentException if {@code skewChunks} is not {@code 0} and either axis does not loop
     */
    public LoopSpans withSkew(int skewChunks) {
        return new LoopSpans(SpansBridge.withSkew(this.spans, skewChunks));
    }

    /** How far crossing the Z seam shifts X, in chunks, within {@code (-xWidth/2, xWidth/2]}; {@code 0} unskewed. */
    public int skewChunks() {
        return this.spans.skewChunks();
    }

    /** Whether this axis loops. {@link Direction.Axis#Y} always answers {@code false}. */
    public boolean loops(Direction.Axis axis) {
        return SpansBridge.loops(this.spans, axis);
    }

    /**
     * The first chunk inside the world on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int minChunk(Direction.Axis axis) {
        return SpansBridge.minChunk(this.spans, axis);
    }

    /**
     * The first chunk past the world on a looping axis (exclusive bound).
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int maxChunk(Direction.Axis axis) {
        return SpansBridge.maxChunk(this.spans, axis);
    }

    /**
     * The world's width in chunks on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int chunkWidth(Direction.Axis axis) {
        return SpansBridge.getWidthChunks(this.spans, axis);
    }

    /** Whether both axes loop over the same number of chunks. */
    public boolean isSquare() {
        return SpansBridge.isSquare(this.spans);
    }

    /**
     * These spans with every looping axis and the skew divided by {@code scale} and re-centered — how the nether of a
     * shaped world is normally derived from its overworld, so that the vanilla portal ratio still lands inside the
     * world and every copy of an overworld position maps onto a copy of the same nether position.
     *
     * @throws IllegalArgumentException if {@code scale} is not positive, leaves a looping axis narrower than a chunk,
     *         or does not divide the skew
     */
    public LoopSpans scaledDown(int scale) {
        return new LoopSpans(SpansBridge.scaledDown(this.spans, scale));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LoopSpans loopSpans && this.spans.equals(loopSpans.spans);
    }

    @Override
    public int hashCode() {
        return this.spans.hashCode();
    }

    @Override
    public String toString() {
        return SpansBridge.text(this.spans);
    }
}
