package com.exoticworlds.compat.lithostitched;

import com.exoticworlds.core.WorldFold;

import dev.worldgen.lithostitched.impl.worldgen.densityfunction.CellularDensityFunction;

public record ClosedGrid(WorldFold fold, CellLap lap, CellularDensityFunction.GridDimensions dimensions) {
    public static ClosedGrid of(WorldFold fold, CellularDensityFunction.GridDimensions dimensions) {
        CellLap lap = CellLap.of(fold, dimensions.horizontal());
        return new ClosedGrid(fold, lap, new CellularDensityFunction.GridDimensions(lap.width(),
                dimensions.vertical()));
    }
}
