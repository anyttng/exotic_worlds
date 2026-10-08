package com.exoticworlds.engine.noise;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class QuantizedCoordinates {
    public static DensityFunction.FunctionContext inBlocks(TilingCellGrid grid, int blockX, int cellY, int blockZ) {
        return new DensityFunction.SinglePointContext(
                grid.cellOriginX(blockX, blockZ), cellY, grid.cellOriginZ(blockZ));
    }

    private QuantizedCoordinates() {
    }
}
