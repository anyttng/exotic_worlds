package com.exoticworlds.shape;

import com.exoticworlds.platform.Platforms;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

public final class WorldOptionSetup {

    public static void registerAll() {
        registerAll(Platforms.get().isClient());
    }

    public static void registerAll(boolean client) {
        CompactBiomes.register(client);
        GuaranteedLand.register(client);
        GuaranteedNetherComplexes.register(client);
    }

    private WorldOptionSetup() {
    }
}
