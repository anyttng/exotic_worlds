package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;

public final class ClimateScaleCompression {
    public static final double NO_COMPRESSION = 1.0;

    public static final double CELLS_PER_LAP = 1.5;

    private static final double HORIZONTAL_SHARE = 0.0;

    static final int UNBOUNDED_LAP = 0;

    public static boolean compressible(WorldFold fold, double verticalShare) {
        return verticalShare == HORIZONTAL_SHARE && lapBlocks(fold) != UNBOUNDED_LAP;
    }

    public static double fitted(WorldFold fold, DoubleList amplitudes, double lowestFreqInputFactor,
            double baseScale) {
        return fittedToCells(weightedCellsPerLap(amplitudes, lapBlocks(fold) * baseScale * lowestFreqInputFactor));
    }

    public static double fittedToCells(double cellsPerLap) {
        return cellsPerLap <= 0.0 || cellsPerLap >= CELLS_PER_LAP
                ? NO_COMPRESSION
                : CELLS_PER_LAP / cellsPerLap;
    }

    static int lapBlocks(WorldFold fold) {
        WrapDomain xDomain = fold.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = fold.blockDomain(Direction.Axis.Z);
        if (xDomain.loops() && zDomain.loops()) {
            return Math.min(xDomain.domainLength, zDomain.domainLength);
        }

        if (xDomain.loops()) {
            return xDomain.domainLength;
        }

        return zDomain.loops() ? zDomain.domainLength : UNBOUNDED_LAP;
    }

    private static double weightedCellsPerLap(DoubleList amplitudes, double lowestOctaveCells) {
        double weighted = 0.0;
        double weight = 0.0;
        double cells = lowestOctaveCells;

        for (int i = 0; i < amplitudes.size(); i++) {
            double amplitude = amplitudes.getDouble(i);
            if (amplitude != 0.0) {
                double square = amplitude * amplitude;
                weighted += square * cells;
                weight += square;
            }

            cells *= 2.0;
        }

        return weight == 0.0 ? 0.0 : weighted / weight;
    }

    private ClimateScaleCompression() {
    }
}
