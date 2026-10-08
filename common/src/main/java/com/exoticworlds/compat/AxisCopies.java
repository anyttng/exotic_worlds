package com.exoticworlds.compat;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;

public final class AxisCopies {
    public static final AxisCopies UNBOUNDED = new AxisCopies(null);

    private final @Nullable WrapDomain domain;

    private AxisCopies(@Nullable WrapDomain domain) {
        this.domain = domain;
    }

    public static AxisCopies of(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis) ? looped(shape.minBlock(axis), shape.widthBlocks(axis)) : UNBOUNDED;
    }

    public static AxisCopies ofChunks(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis) ? looped(shape.minChunk(axis), shape.widthChunks(axis)) : UNBOUNDED;
    }

    public static AxisCopies looped(int min, int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("A looped axis needs a positive width, got " + width);
        }

        return new AxisCopies(new WrapDomain(min, min + width));
    }

    public static int[] lapRange(int first, int last) {
        if (last < first) {
            return new int[0];
        }

        int[] laps = new int[last - first + 1];
        for (int i = 0; i < laps.length; i++) {
            laps[i] = first + i;
        }

        return laps;
    }

    public boolean loops() {
        return this.domain != null;
    }

    public int min() {
        return looped().lowerBound;
    }

    public int max() {
        return looped().upperBound;
    }

    public int width() {
        return this.domain == null ? 0 : this.domain.domainLength;
    }

    public int[] laps(int spanMin, int spanMax) {
        if (this.domain == null) {
            return new int[] {0};
        }

        int first = Math.floorDiv(spanMin - this.domain.lowerBound, this.domain.domainLength);
        int last = Math.floorDiv(spanMax - 1 - this.domain.lowerBound, this.domain.domainLength);
        return lapRange(first, last);
    }

    public int[] seams(int spanMin, int spanMax) {
        if (this.domain == null) {
            return new int[0];
        }

        int[] laps = laps(spanMin, spanMax);
        if (laps.length == 0) {
            return laps;
        }

        int[] seams = new int[laps.length + 1];
        for (int i = 0; i < laps.length; i++) {
            seams[i] = this.domain.lowerBound + offset(laps[i]);
        }

        seams[laps.length] = this.domain.upperBound + offset(laps[laps.length - 1]);
        return seams;
    }

    public int reach(int spanMin, int spanMax) {
        int[] laps = laps(spanMin, spanMax);
        return laps.length == 0 ? 0 : Math.max(-laps[0], laps[laps.length - 1]);
    }

    public double clampView(double center, double halfSpan) {
        if (this.domain == null) {
            return center;
        }

        double low = this.domain.lowerBound + halfSpan;
        double high = this.domain.upperBound - halfSpan;
        return low > high
                ? (this.domain.lowerBound + this.domain.upperBound) / 2.0
                : Math.max(low, Math.min(high, center));
    }

    public int offset(int lap) {
        return lap * width();
    }

    public int wrap(int coord) {
        return this.domain == null ? coord : this.domain.wrap(coord);
    }

    public int nearest(int reference, int coord) {
        return this.domain == null ? coord : this.domain.unwrapAround(reference, coord);
    }

    public int withinOneLap(int anchor, int coord) {
        if (this.domain == null) {
            return coord;
        }

        int reach = this.domain.domainLength - 1;
        return Math.max(anchor - reach, Math.min(anchor + reach, coord));
    }

    public int clipMin(int spanMin) {
        return this.domain == null ? spanMin : Math.max(spanMin, this.domain.lowerBound);
    }

    public int clipMax(int spanMax) {
        return this.domain == null ? spanMax : Math.min(spanMax, this.domain.upperBound);
    }

    private WrapDomain looped() {
        if (this.domain == null) {
            throw new IllegalStateException("The axis does not loop — check loops() first");
        }

        return this.domain;
    }
}
