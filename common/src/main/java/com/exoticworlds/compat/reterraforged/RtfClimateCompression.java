package com.exoticworlds.compat.reterraforged;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.shape.climate.ClimateCompression;

import net.minecraft.core.Direction;

public final class RtfClimateCompression {
    static final double LATTICE_HALF_CELLS = 0.449;

    static final double TEMPERATURE_HALF_Z = 0.951;

    static final double MOISTURE_HALF_X = 0.452;

    static final double MOISTURE_HALF_Z = 0.233;

    private static final boolean CLIMATE_FIELD = true;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final Direction.Axis[] HORIZONTAL = {Direction.Axis.X, Direction.Axis.Z};

    public static double factor(WorldFold fold, RtfClimateScales scales) {
        return ClimateCompression.factor(fold, CLIMATE_FIELD, HORIZONTAL_SHARE, () -> fitted(fold, scales));
    }

    static double fitted(WorldFold fold, RtfClimateScales scales) {
        double fit = ClimateScaleCompression.NO_COMPRESSION;
        for (Direction.Axis axis : HORIZONTAL) {
            WrapDomain domain = fold.blockDomain(axis);
            if (domain.loops()) {
                double cellsPerLap = domain.domainLength * LATTICE_HALF_CELLS / halfBlocks(axis, scales);
                fit = Math.max(fit, ClimateScaleCompression.fittedToCells(cellsPerLap));
            }
        }

        return fit;
    }

    static double halfBlocks(Direction.Axis axis, RtfClimateScales scales) {
        double moisture = (axis == Direction.Axis.X ? MOISTURE_HALF_X : MOISTURE_HALF_Z)
                * scales.toroidal$moistureScale();
        double coarsest = axis == Direction.Axis.X
                ? moisture
                : Math.max(moisture, TEMPERATURE_HALF_Z * scales.toroidal$temperatureScale());
        return coarsest / scales.toroidal$biomeFrequency();
    }

    private RtfClimateCompression() {
    }
}
