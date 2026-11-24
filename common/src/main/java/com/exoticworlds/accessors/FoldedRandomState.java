package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.BaseHeights;
import com.exoticworlds.engine.noise.AquiferCells;
import com.exoticworlds.engine.noise.FoldedCompileContext;

public interface FoldedRandomState {
    @Nullable FoldedCompileContext exotic_worlds$getCompileContext();

    @Nullable BaseHeights exotic_worlds$getBaseHeights();

    @Nullable AquiferCells exotic_worlds$getAquiferCells();

    void exotic_worlds$setAquiferCells(AquiferCells cells);
}
