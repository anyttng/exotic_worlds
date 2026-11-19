package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;

import net.minecraft.core.Direction;

public enum ShapeKind {
    NORMAL,
    TORUS,
    CYLINDER,
    FROM_OVERWORLD;

    public static ShapeKind of(@Nullable LoopSpans spans) {
        if (spans == null) {
            return NORMAL;
        }

        boolean x = spans.loops(Direction.Axis.X);
        boolean z = spans.loops(Direction.Axis.Z);
        if (x && z) {
            return TORUS;
        }

        return x || z ? CYLINDER : NORMAL;
    }
}
