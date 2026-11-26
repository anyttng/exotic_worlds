package com.exoticworlds.bridge.v2.shape;

import com.exoticworlds.api.v2.feature.FeatureV2;
import com.exoticworlds.api.v2.shape.DimensionScopeV2;
import com.exoticworlds.api.v2.shape.ShapeV2;
import com.exoticworlds.bridge.v2.feature.FeatureBridge;
import com.exoticworlds.core.feature.DimensionScope;
import com.exoticworlds.shape.dimension.AttachedFeatures;
import com.exoticworlds.shape.dimension.DeclaredShapes;

import net.minecraft.resources.Identifier;

public final class ShapeBridge {

    public static void register(ShapeV2 shape) {
        BridgedParameters parameters = new BridgedParameters(shape);
        DeclaredShapes.register(new BridgedShape(shape, parameters));
        parameters.registerOptions();
    }

    public static void attach(Identifier shape, FeatureV2 feature, DimensionScopeV2 scope) {
        AttachedFeatures.attach(shape, FeatureBridge.internalOf(feature),
                scope.isAll() ? DimensionScope.ALL : DimensionScope.of(scope.getDimensions()));
    }

    public static Identifier getTorus() {
        return DeclaredShapes.TORUS;
    }

    public static Identifier getCylinder() {
        return DeclaredShapes.CYLINDER;
    }

    private ShapeBridge() {
    }
}
