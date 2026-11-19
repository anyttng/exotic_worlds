package com.exoticworlds.shape;

import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.layout.LayoutShape;

public final class WorldShapeSetup {

    public static void registerAll() {
        boolean client = Platforms.get().isClient();
        LayoutShape.register(client);
    }

    private WorldShapeSetup() {
    }
}
