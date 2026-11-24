package com.exoticworlds.shape;

import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

public final class WorldOptionSetup {

    public static void registerAll() {
        ShapeOptionRegistry.register(SpawnPoint.OPTION);
        CompactBiomes.register();
        GuaranteedLand.register();
        GuaranteedNetherComplexes.register();
    }

    private WorldOptionSetup() {
    }
}
