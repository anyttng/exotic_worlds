package com.exoticworlds.shape;

import com.exoticworlds.core.registry.RegistrationBoundary;
import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.layout.LayoutShape;

public final class WorldShapeSetup {

    public static void registerAll() {
        boolean isClient = Platforms.get().isClient();
        LayoutShape.register(isClient);
        RegistrationBoundary.STARTUP.atFirstClose(DeclaredShapes::warnOfUnknownOptionShapes);
    }

    private WorldShapeSetup() {
    }
}
