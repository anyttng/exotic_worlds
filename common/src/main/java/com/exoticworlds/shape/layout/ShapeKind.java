package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.shape.cylinder.Cylinder;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.torus.Torus;

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

    public @Nullable DimensionShape builtIn() {
        return switch (this) {
            case TORUS -> Torus.SHAPE;
            case CYLINDER -> Cylinder.SHAPE;
            case NORMAL, FROM_OVERWORLD -> null;
        };
    }
}
