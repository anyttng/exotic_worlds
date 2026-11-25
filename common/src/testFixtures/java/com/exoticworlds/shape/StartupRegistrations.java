package com.exoticworlds.shape;

import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.layout.LayoutShape;

public final class StartupRegistrations {
    public static synchronized void options() {
        if (!ShapeOptionRegistry.getAll().contains(SpawnPoint.OPTION)) {
            WorldOptionSetup.registerAll();
        }
    }

    public static synchronized void layoutShape() {
        if (WorldShapes.getShapes().stream().noneMatch(shape -> shape.id().equals(LayoutShape.ID))) {
            LayoutShape.register(false);
        }
    }

    private StartupRegistrations() {
    }
}
