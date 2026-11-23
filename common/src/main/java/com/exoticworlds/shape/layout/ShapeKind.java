package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FlatShape;

import net.minecraft.core.Direction;

public enum ShapeKind {
    NORMAL,
    TORUS,
    CYLINDER,
    FROM_OVERWORLD;

    public static ShapeKind of(@Nullable FlatShape shape) {
        if (shape == null) {
            return NORMAL;
        }

        boolean x = shape.bounds().loops(Direction.Axis.X);
        boolean z = shape.bounds().loops(Direction.Axis.Z);
        if (x && z) {
            return TORUS;
        }

        return x || z ? CYLINDER : NORMAL;
    }
}
