package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.AquiferCells;

public interface AquiferCellsHolder {
    @Nullable AquiferCells exotic_worlds$getAquiferCells();

    void exotic_worlds$setAquiferCells(AquiferCells cells);
}
