package com.exoticworlds.bridge.v2.shape;

import com.exoticworlds.api.v2.shape.DimensionShapeV2;
import com.exoticworlds.shape.dimension.DeclaredShapes;

import net.minecraft.resources.Identifier;

public final class ShapeBridge {

    public static void register(DimensionShapeV2 shape) {
        DeclaredShapes.register(new DimensionShapeView(shape));
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
