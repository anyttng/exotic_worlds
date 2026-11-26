package com.exoticworlds.compat.terraforged;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.MapMaker;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.engine.noise.ForeignFieldCompression;

import net.minecraft.core.Direction;

public final class RtfClimateCompression {
    static final double LATTICE_HALF_CELLS = 0.449;

    static final double TEMPERATURE_HALF_Z = 0.951;

    static final double MOISTURE_HALF_X = 0.452;

    static final double MOISTURE_HALF_Z = 0.233;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final Direction.Axis[] HORIZONTAL = {Direction.Axis.X, Direction.Axis.Z};

    private static final Map<WorldFold, Double> BY_FOLD = new MapMaker().weakKeys().makeMap();

    public static double factor(WorldFold fold, RtfClimateScales scales) {
        return ForeignFieldCompression.ofField(fold, HORIZONTAL_SHARE, () -> fitted(fold, scales));
    }

    public static double carry(WorldFold fold, RtfClimateScales scales) {
        double factor = factor(fold, scales);
        BY_FOLD.put(fold, factor);
        return factor;
    }

    public static double of(@Nullable WorldFold fold) {
        return fold == null ? ClimateScaleCompression.NO_COMPRESSION
                : BY_FOLD.getOrDefault(fold, ClimateScaleCompression.NO_COMPRESSION);
    }

    static double fitted(WorldFold fold, RtfClimateScales scales) {
        double fit = ClimateScaleCompression.NO_COMPRESSION;
        TranslationLattice lattice = fold.getBlockLattice();
        for (Direction.Axis axis : HORIZONTAL) {
            WrapDomain domain = lattice.getAxis(axis);
            if (domain.isLooped()) {
                double cellsPerLap = domain.domainLength * LATTICE_HALF_CELLS / halfBlocks(axis, scales);
                fit = Math.max(fit, ClimateScaleCompression.fittedToCells(cellsPerLap));
            }
        }

        return fit;
    }

    static double halfBlocks(Direction.Axis axis, RtfClimateScales scales) {
        double moisture = (axis == Direction.Axis.X ? MOISTURE_HALF_X : MOISTURE_HALF_Z)
                * scales.exotic_worlds$getMoistureScale();
        double coarsest = axis == Direction.Axis.X
                ? moisture
                : Math.max(moisture, TEMPERATURE_HALF_Z * scales.exotic_worlds$getTemperatureScale());
        return coarsest / scales.exotic_worlds$getBiomeFrequency();
    }

    private RtfClimateCompression() {
    }
}
