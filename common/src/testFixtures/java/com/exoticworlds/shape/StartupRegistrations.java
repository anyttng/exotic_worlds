package com.exoticworlds.shape;

import com.exoticworlds.core.gen.GenerationMoments;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.world.SpawnPoint;

public final class StartupRegistrations {
    public static synchronized void options() {
        if (!ShapeOptionRegistry.getAll().contains(SpawnPoint.OPTION)) {
            WorldOptionSetup.registerAll();
        }
    }

    public static synchronized void generationHooks() {
        if (!GenerationMoments.hasStructureStartHooks()) {
            GenerationHookSetup.registerAll();
        }
    }

    private StartupRegistrations() {
    }
}
