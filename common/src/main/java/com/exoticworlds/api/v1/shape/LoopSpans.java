package com.exoticworlds.api.v1.shape;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

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
    private static final String ASCENDING_SPAN_REQUIRED = "A looping axis needs minChunk < maxChunk, got [";
    private static final String DOES_NOT_LOOP = " does not loop — check loops(axis) first";
    private static final String AXIS = "Axis ";
    private static final String POSITIVE_SCALE_REQUIRED = "A scale is positive, got ";
    private static final String SKEW_NOT_DIVISIBLE = "A skew of %d chunks does not divide by the scale %d";

    /** Neither axis loops — an ordinary world. */
    public static final LoopSpans NONE = new LoopSpans(WorldLoopBounds.UNBOUNDED);

    private final WorldLoopBounds bounds;
    private final int skewChunks;

    LoopSpans(WorldLoopBounds bounds) {
        this(bounds, FlatShape.NO_SKEW);
    }

    LoopSpans(WorldLoopBounds bounds, int skewChunks) {
        ascending(bounds.x());
        ascending(bounds.z());
        this.bounds = bounds;
        this.skewChunks = FlatShape.latticeTorus(bounds, skewChunks).skewChunks();
    }

    /** Both axes looping over {@code chunkWidth} chunks, centred on the origin. */
    public static LoopSpans ofWidth(int chunkWidth) {
        return new LoopSpans(WorldLoopBounds.ofWidth(chunkWidth));
    }

    /** X looping over {@code xChunkWidth} chunks and Z over {@code zChunkWidth}, both centred on the origin. */
    public static LoopSpans ofWidths(int xChunkWidth, int zChunkWidth) {
        return new LoopSpans(WorldLoopBounds.ofWidths(xChunkWidth, zChunkWidth));
    }

    /** One axis looping over {@code chunkWidth} chunks, centred on the origin; the other stays unbounded. */
    public static LoopSpans ofWidth(Direction.Axis axis, int chunkWidth) {
        return new LoopSpans(WorldLoopBounds.ofWidth(axis, chunkWidth));
    }

    /** One axis looping over the half-open chunk span {@code [minChunk, maxChunk)}; the other stays unbounded. */
    public static LoopSpans of(Direction.Axis axis, int minChunk, int maxChunk) {
        return NONE.and(axis, minChunk, maxChunk);
    }

    /** These spans with {@code axis} looping over {@code [minChunk, maxChunk)} as well. */
    public LoopSpans and(Direction.Axis axis, int minChunk, int maxChunk) {
        return new LoopSpans(this.bounds.with(axis, new AxisBounds.Looped(minChunk, maxChunk)), this.skewChunks);
    }

    /**
     * These spans with crossing the Z seam also shifting X by {@code skewChunks} chunks — a lattice torus. Any whole
     * number of X widths may be added to the skew without changing the world; {@link #skewChunks()} answers it
     * brought into {@code (-xWidth/2, xWidth/2]}, and a skew of {@code 0} is the plain torus.
     *
     * @throws IllegalArgumentException if {@code skewChunks} is not {@code 0} and either axis does not loop
     */
    public LoopSpans withSkew(int skewChunks) {
        return new LoopSpans(this.bounds, skewChunks);
    }

    /** How far crossing the Z seam shifts X, in chunks, within {@code (-xWidth/2, xWidth/2]}; {@code 0} unskewed. */
    public int skewChunks() {
        return this.skewChunks;
    }

    /** Whether this axis loops. {@link Direction.Axis#Y} always answers {@code false}. */
    public boolean loops(Direction.Axis axis) {
        return axis != Direction.Axis.Y && this.bounds.loops(axis);
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
        return this.bounds.isSquare();
    }

    /**
     * These spans with every looping axis and the skew divided by {@code scale} and re-centred — how the nether of a
     * shaped world is normally derived from its overworld, so that the vanilla portal ratio still lands inside the
     * world and every copy of an overworld position maps onto a copy of the same nether position.
     *
     * @throws IllegalArgumentException if {@code scale} is not positive, leaves a looping axis narrower than a chunk,
     *         or does not divide the skew
     */
    public LoopSpans scaledDown(int scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException(POSITIVE_SCALE_REQUIRED + scale);
        }

        if (this.skewChunks % scale != 0) {
            throw new IllegalArgumentException(String.format(SKEW_NOT_DIVISIBLE, this.skewChunks, scale));
        }

        return new LoopSpans(this.bounds.scaledDown(scale), this.skewChunks / scale);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LoopSpans spans && this.bounds.equals(spans.bounds)
                && this.skewChunks == spans.skewChunks;
    }

    @Override
    public int hashCode() {
        return 31 * this.bounds.hashCode() + this.skewChunks;
    }

    @Override
    public String toString() {
        return "LoopSpans[x=" + this.bounds.x().spanText() + ", z=" + this.bounds.z().spanText()
                + ", skew=" + this.skewChunks + "]";
    }

    WorldLoopBounds bounds() {
        return this.bounds;
    }

    private AxisBounds.Looped looping(Direction.Axis axis) {
        if (this.bounds.axis(axis) instanceof AxisBounds.Looped looped) {
            return looped;
        }

        throw new IllegalArgumentException(AXIS + axis + DOES_NOT_LOOP);
    }

    private static void ascending(AxisBounds axis) {
        if (axis instanceof AxisBounds.Looped looped && looped.minChunk() >= looped.maxChunk()) {
            throw new IllegalArgumentException(ASCENDING_SPAN_REQUIRED
                    + looped.minChunk() + ", " + looped.maxChunk() + ")");
        }
    }
}
