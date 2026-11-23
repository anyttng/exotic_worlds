package com.exoticworlds.bridge.v2.shape;

import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.shape.layout.DeclaredShapes;

import net.minecraft.resources.Identifier;

public final class ShapeBridge {

    public static void register(DimensionShape shape) {
        DeclaredShapes.register(shape);
    }

    public static Identifier torus() {
        return DeclaredShapes.TORUS;
    }

    public static Identifier cylinder() {
        return DeclaredShapes.CYLINDER;
    }

    private ShapeBridge() {
    }
}
