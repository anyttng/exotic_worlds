package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.AquiferCells;
import com.exoticworlds.engine.noise.FoldedCompileContext;

public interface FoldedRandomState {
    @Nullable FoldedCompileContext toroidal$compileContext();

    @Nullable AquiferCells toroidal$aquiferCells();

    void toroidal$aquiferCells(AquiferCells cells);
}
