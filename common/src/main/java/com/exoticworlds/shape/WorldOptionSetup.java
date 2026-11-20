package com.exoticworlds.shape;

import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

public final class WorldOptionSetup {

    public static void registerAll() {
        WorldOptions.register(SpawnPoint.OPTION);
        CompactBiomes.register();
        GuaranteedLand.register();
        GuaranteedNetherComplexes.register();
    }

    private WorldOptionSetup() {
    }
}
