package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.Divisors;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

public record TilingCellGrid(WorldFold transformer, int xCellWidth, int zCellWidth, int skew, int rowsPerLap) {
    private static final int SMALLEST_CELL_COUNT = 1;

    public static TilingCellGrid of(WorldFold transformer, int vanillaCellWidth) {
        TranslationLattice lattice = transformer.blockLattice();
        int xCellWidth = tilingWidth(lattice.x(), vanillaCellWidth);
        int zCellWidth = tilingWidth(lattice.z(), vanillaCellWidth);
        int rowsPerLap = lattice.isSkewed() ? lattice.z().domainLength / zCellWidth : SMALLEST_CELL_COUNT;
        return new TilingCellGrid(transformer, xCellWidth, zCellWidth, lattice.skew(), rowsPerLap);
    }

    public static TilingCellGrid resolve(@Nullable TilingCellGrid cached,
            WorldFold transformer, int vanillaCellWidth) {
        return cached != null && cached.transformer == transformer
                ? cached
                : of(transformer, vanillaCellWidth);
    }

    public int cellOriginX(int blockX, int blockZ) {
        if (this.skew == TranslationLattice.NO_SKEW) {
            return Math.floorDiv(blockX, this.xCellWidth) * this.xCellWidth;
        }

        int rowOffset = (int) Math.floorDiv((long) Math.floorDiv(blockZ, this.zCellWidth) * this.skew, this.rowsPerLap);
        return Math.floorDiv(blockX - rowOffset, this.xCellWidth) * this.xCellWidth + rowOffset;
    }

    public int cellOriginZ(int blockZ) {
        return Math.floorDiv(blockZ, this.zCellWidth) * this.zCellWidth;
    }

    static int tilingWidth(WrapDomain domain, int vanillaCellWidth) {
        if (!domain.loops()) {
            return vanillaCellWidth;
        }

        int width = domain.domainLength;
        double vanillaCellCount = (double) width / vanillaCellWidth;
        int cellCount = SMALLEST_CELL_COUNT;

        for (int divisor : Divisors.of(width)) {
            cellCount = nearer(cellCount, divisor, vanillaCellCount);
        }

        return width / cellCount;
    }

    private static int nearer(int chosen, int candidate, double vanillaCellCount) {
        double chosenDistance = Math.abs(chosen - vanillaCellCount);
        double candidateDistance = Math.abs(candidate - vanillaCellCount);
        if (candidateDistance < chosenDistance) {
            return candidate;
        }

        return candidateDistance > chosenDistance ? chosen : Math.max(chosen, candidate);
    }
}
