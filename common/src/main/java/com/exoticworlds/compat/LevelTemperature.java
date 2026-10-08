package com.exoticworlds.compat;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class LevelTemperature {
    private volatile @Nullable Resolved resolved;

    public DensityFunction of(@Nullable ServerLevel level) {
        DensityFunction own = LevelClimateCompression.routerTemperatureOf(level);
        DensityFunction overworld = LevelClimateCompression.overworldTemperatureOf(level);
        Resolved held = this.resolved;
        if (held != null && held.own() == own && held.overworld() == overworld) {
            return held.temperature();
        }

        DensityFunction temperature = LevelClimateCompression.temperatureOf(own, overworld);
        this.resolved = new Resolved(own, overworld, temperature);
        return temperature;
    }

    private record Resolved(DensityFunction own, DensityFunction overworld, DensityFunction temperature) {
    }
}
