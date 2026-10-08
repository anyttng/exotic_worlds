package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.AquiferCells;

public interface AquiferCellsHolder {
    @Nullable AquiferCells toroidal$aquiferCells();

    void toroidal$aquiferCells(AquiferCells cells);
}
