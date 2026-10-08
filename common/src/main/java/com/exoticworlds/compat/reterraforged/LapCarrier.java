package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

public interface LapCarrier {
    @Nullable WorldFold toroidal$fold();

    void toroidal$carryFold(WorldFold fold);

    double toroidal$climateCompression();
}
