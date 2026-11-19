package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.BaseHeights;
import com.exoticworlds.engine.noise.AquiferCells;
import com.exoticworlds.engine.noise.FoldedCompileContext;

public interface FoldedRandomState {
    @Nullable FoldedCompileContext exotic_worlds$compileContext();

    @Nullable BaseHeights exotic_worlds$baseHeights();

    @Nullable AquiferCells exotic_worlds$aquiferCells();

    void exotic_worlds$aquiferCells(AquiferCells cells);
}
