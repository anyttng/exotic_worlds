package com.exoticworlds.api.v2.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.bridge.v2.shape.SpansBridge;

import net.minecraft.core.Direction;

/**
 * What a shape states about one dimension: which horizontal axes loop, over what span of chunks, and, where both loop,
 * how far crossing the Z seam shifts X. An axis with no span does not loop and stretches to the vanilla world border.
 * {@link Direction.Axis#Y} never loops and is refused by every member that takes an axis.
 *
 * <p>Which flat surface the world becomes is read off the spans, not declared: both axes give a torus (a lattice torus
 * where it is skewed), one gives a cylinder, neither gives an ordinary world. A skew other than {@code 0} where either
 * axis does not loop is refused with an {@link IllegalArgumentException}. Immutable; every wither hands back a new
 * instance.</p>
 *
 * @param x the span X loops over, or {@code null} where X does not loop
 * @param z the span Z loops over, or {@code null} where Z does not loop
 * @param skewChunks how far crossing the Z seam shifts X, in chunks; any whole number of X widths may be added without
 *        changing the world, and the spans keep it brought into {@code (-xWidth/2, xWidth/2]}, {@code 0} unskewed
 */
public record LoopSpansV2(@Nullable Span x, @Nullable Span z, int skewChunks) {
    private static final String AXIS = "Axis ";
    private static final String DOES_NOT_LOOP = " does not loop, check loops(axis) first";
    private static final String NOT_HORIZONTAL = " is not a horizontal axis";

    /** Neither axis loops: an ordinary world. */
    public static final LoopSpansV2 NONE = new LoopSpansV2(null, null, 0);

    /**
     * One looping axis as a half-open span of chunks: {@code minChunk} is the first chunk inside the world,
     * {@code maxChunk} the first one past it. A span whose {@code minChunk} is not below its {@code maxChunk} is
     * refused with an {@link IllegalArgumentException}.
     *
     * @param minChunk the first chunk inside the world
     * @param maxChunk the first chunk past the world
     */
    public record Span(int minChunk, int maxChunk) {
        public Span {
            SpansBridge.requireAscending(minChunk, maxChunk);
        }

        /** The span's width in chunks. */
        public int chunkWidth() {
            return this.maxChunk - this.minChunk;
        }
    }

    public LoopSpansV2 {
        skewChunks = SpansBridge.normalizedSkew(x, z, skewChunks);
    }

    /**
     * The narrowest a looping axis may be in any dimension, in chunks. A dimension whose spans hold a narrower axis is
     * refused, so scale a nether down only as far as this allows.
     */
    public static int minChunkWidth() {
        return SpansBridge.minChunkWidth();
    }

    /** Both axes looping over {@code chunkWidth} chunks, centered on the origin. */
    public static LoopSpansV2 ofWidth(int chunkWidth) {
        return SpansBridge.ofWidth(chunkWidth);
    }

    /** X looping over {@code xChunkWidth} chunks and Z over {@code zChunkWidth}, both centered on the origin. */
    public static LoopSpansV2 ofWidths(int xChunkWidth, int zChunkWidth) {
        return SpansBridge.ofWidths(xChunkWidth, zChunkWidth);
    }

    /** One axis looping over {@code chunkWidth} chunks, centered on the origin; the other does not loop. */
    public static LoopSpansV2 ofWidth(Direction.Axis axis, int chunkWidth) {
        return SpansBridge.ofWidth(axis, chunkWidth);
    }

    /** One axis looping over the half-open chunk span {@code [minChunk, maxChunk)}; the other does not loop. */
    public static LoopSpansV2 of(Direction.Axis axis, int minChunk, int maxChunk) {
        return NONE.and(axis, minChunk, maxChunk);
    }

    /** These spans with {@code axis} looping over {@code [minChunk, maxChunk)} as well. */
    public LoopSpansV2 and(Direction.Axis axis, int minChunk, int maxChunk) {
        Span span = new Span(minChunk, maxChunk);
        return switch (axis) {
            case X -> new LoopSpansV2(span, this.z, this.skewChunks);
            case Z -> new LoopSpansV2(this.x, span, this.skewChunks);
            case Y -> throw new IllegalArgumentException(AXIS + axis + NOT_HORIZONTAL);
        };
    }

    /**
     * These spans with crossing the Z seam also shifting X by {@code skewChunks} chunks: a lattice torus.
     *
     * @throws IllegalArgumentException if {@code skewChunks} is not {@code 0} and either axis does not loop
     */
    public LoopSpansV2 withSkew(int skewChunks) {
        return new LoopSpansV2(this.x, this.z, skewChunks);
    }

    /** Whether this axis loops. {@link Direction.Axis#Y} always answers {@code false}. */
    public boolean loops(Direction.Axis axis) {
        return span(axis) != null;
    }

    /**
     * The first chunk inside the world on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int minChunk(Direction.Axis axis) {
        return looping(axis).minChunk();
    }

    /**
     * The first chunk past the world on a looping axis (exclusive bound).
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int maxChunk(Direction.Axis axis) {
        return looping(axis).maxChunk();
    }

    /**
     * The world's width in chunks on a looping axis.
     *
     * @throws IllegalArgumentException if {@code axis} does not loop
     */
    public int chunkWidth(Direction.Axis axis) {
        return looping(axis).chunkWidth();
    }

    /** Whether both axes loop over the same number of chunks. */
    public boolean isSquare() {
        return this.x != null && this.z != null && this.x.chunkWidth() == this.z.chunkWidth();
    }

    /**
     * These spans with every looping axis and the skew divided by {@code scale} and re-centered: how the nether of a
     * shaped world is normally derived from its overworld, so that the vanilla portal ratio still lands inside the
     * world and every copy of an overworld position maps onto a copy of the same nether position.
     *
     * @throws IllegalArgumentException if {@code scale} is not positive, leaves a looping axis narrower than a chunk,
     *         or does not divide the skew
     */
    public LoopSpansV2 scaledDown(int scale) {
        return SpansBridge.scaledDown(this, scale);
    }

    private @Nullable Span span(Direction.Axis axis) {
        return switch (axis) {
            case X -> this.x;
            case Z -> this.z;
            case Y -> null;
        };
    }

    private Span looping(Direction.Axis axis) {
        Span span = span(axis);
        if (span == null) {
            throw new IllegalArgumentException(AXIS + axis + DOES_NOT_LOOP);
        }

        return span;
    }
}
