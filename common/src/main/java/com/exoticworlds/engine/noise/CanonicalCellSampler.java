package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.GenerationFoldContext.Context;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class CanonicalCellSampler {
    public interface Sample {
        double at(DensityFunction.FunctionContext cell);
    }

    private final int vanillaCellWidthBlocks;

    private @Nullable TilingCellGrid grid;

    public CanonicalCellSampler(int vanillaCellWidthBlocks) {
        this.vanillaCellWidthBlocks = vanillaCellWidthBlocks;
    }

    public double sample(DensityFunction.FunctionContext cell, int blockX, int blockZ, Sample sample) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return sample.at(cell);
        }

        TilingCellGrid grid = TilingCellGrid.resolve(this.grid, generation.getFold(), this.vanillaCellWidthBlocks);
        this.grid = grid;

        try (Context.DivisorScope _ = generation.withDivisors(grid.xCellWidthBlocks(), grid.zCellWidthBlocks())) {
            return sample.at(QuantizedCoordinates.inBlocks(grid, blockX, cell.blockY(), blockZ));
        }
    }
}
