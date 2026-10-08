package com.exoticworlds.shape;

import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.cylinder.CylinderShape;
import com.exoticworlds.shape.torus.TorusShape;

public final class WorldShapeSetup {

    public static void registerAll() {
        boolean client = Platforms.get().isClient();
        TorusShape.register(client);
        CylinderShape.register(client);
    }

    private WorldShapeSetup() {
    }
}
