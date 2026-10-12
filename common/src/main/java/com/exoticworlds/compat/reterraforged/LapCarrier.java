package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import raccoonman.reterraforged.world.worldgen.GeneratorContext;

public interface LapCarrier {
    static void carry(GeneratorContext context, WorldFold fold) {
        ((LapCarrier) (Object) context.generator.getHeightmap()).toroidal$carryFold(fold);
        RtfLap.activate();
    }

    @Nullable WorldFold toroidal$fold();

    void toroidal$carryFold(WorldFold fold);

    double toroidal$climateCompression();
}
