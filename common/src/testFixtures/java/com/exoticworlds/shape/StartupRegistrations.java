package com.exoticworlds.shape;

import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.core.GenerationMoments;
import com.exoticworlds.core.SpawnPoint;

public final class StartupRegistrations {
    public static synchronized void options() {
        if (!WorldOptions.all().contains(SpawnPoint.OPTION)) {
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
