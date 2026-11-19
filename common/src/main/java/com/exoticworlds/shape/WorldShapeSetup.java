package com.exoticworlds.shape;

import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.custom.CustomShape;
import com.exoticworlds.shape.cylinder.CylinderShape;
import com.exoticworlds.shape.layout.LayoutShape;
import com.exoticworlds.shape.torus.TorusShape;

public final class WorldShapeSetup {

    public static void registerAll() {
        boolean client = Platforms.get().isClient();
        LayoutShape.register(client);
        TorusShape.register(client);
        CylinderShape.register(client);
        CustomShape.register(client);
    }

    private WorldShapeSetup() {
    }
}
