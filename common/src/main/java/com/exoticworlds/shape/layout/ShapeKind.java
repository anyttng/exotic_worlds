package com.exoticworlds.shape.layout;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
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

        boolean isXLooped = shape.bounds().isLooped(Direction.Axis.X);
        boolean isZLooped = shape.bounds().isLooped(Direction.Axis.Z);
        if (isXLooped && isZLooped) {
            return TORUS;
        }

        return isXLooped || isZLooped ? CYLINDER : NORMAL;
    }

    public static List<DimensionShape> getBuiltIns() {
        List<DimensionShape> builtIns = new ArrayList<>();
        for (ShapeKind kind : values()) {
            DimensionShape builtIn = kind.getBuiltIn();
            if (builtIn != null) {
                builtIns.add(builtIn);
            }
        }

        return builtIns;
    }

    public @Nullable DimensionShape getBuiltIn() {
        return switch (this) {
            case TORUS -> Torus.SHAPE;
            case CYLINDER -> Cylinder.SHAPE;
            case NORMAL, FROM_OVERWORLD -> null;
        };
    }
}
