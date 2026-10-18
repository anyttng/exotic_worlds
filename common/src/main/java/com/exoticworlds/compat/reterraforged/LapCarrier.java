package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import raccoonman.reterraforged.world.worldgen.GeneratorContext;

public interface LapCarrier {
    static void carry(GeneratorContext context, WorldFold fold) {
        ((LapCarrier) (Object) context.generator.getHeightmap()).exotic_worlds$carryFold(fold);
        RtfLap.activate();
    }

    @Nullable WorldFold exotic_worlds$fold();

    void exotic_worlds$carryFold(WorldFold fold);

    double exotic_worlds$climateCompression();
}
