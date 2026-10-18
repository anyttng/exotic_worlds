package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.AquiferCells;

public interface AquiferCellsHolder {
    @Nullable AquiferCells exotic_worlds$aquiferCells();

    void exotic_worlds$aquiferCells(AquiferCells cells);
}
