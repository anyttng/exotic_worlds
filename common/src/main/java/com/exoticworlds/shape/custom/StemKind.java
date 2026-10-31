package com.exoticworlds.shape.custom;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.FlatShape;

import net.minecraft.core.Direction;

public enum StemKind {
    NONE,
    TORUS,
    CYLINDER;

    public @Nullable LoopSpans respan(@Nullable LoopSpans current, int defaultChunkWidth) {
        return switch (this) {
            case NONE -> null;
            case TORUS -> current != null && of(current) == TORUS
                    ? current
                    : LoopSpans.ofWidth(current == null ? defaultChunkWidth : loopedWidthOf(current));
            case CYLINDER -> current != null && of(current) == CYLINDER
                    ? current
                    : LoopSpans.ofWidth(Direction.Axis.X, current == null ? defaultChunkWidth : loopedWidthOf(current));
        };
    }

    public static @Nullable StemKind of(@Nullable LoopSpans spans) {
        if (spans == null) {
            return NONE;
        }

        if (spans.skewChunks() != FlatShape.NO_SKEW) {
            return null;
        }

        boolean x = spans.loops(Direction.Axis.X);
        boolean z = spans.loops(Direction.Axis.Z);
        if (x && z) {
            return TORUS;
        }

        return x || z ? CYLINDER : NONE;
    }

    private static int loopedWidthOf(LoopSpans spans) {
        return spans.chunkWidth(spans.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z);
    }
}
