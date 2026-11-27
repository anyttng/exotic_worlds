package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.Divisors;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;

public record TilingCellGrid(WorldFold fold, int xCellWidthBlocks, int zCellWidthBlocks, int skewBlocks, int rowsPerLap)
        implements AquiferCellGrid {
    private static final int SMALLEST_CELL_COUNT = 1;

    public static TilingCellGrid of(WorldFold fold, int vanillaCellWidthBlocks) {
        TranslationLattice lattice = fold.getBlockLattice();
        int xCellWidthBlocks = tilingWidthBlocks(lattice.getX(), vanillaCellWidthBlocks);
        int zCellWidthBlocks = tilingWidthBlocks(lattice.getZ(), vanillaCellWidthBlocks);
        int rowsPerLap = lattice.isSkewed() ? lattice.getZ().domainLength / zCellWidthBlocks : SMALLEST_CELL_COUNT;
        return new TilingCellGrid(fold, xCellWidthBlocks, zCellWidthBlocks, lattice.getSkew(), rowsPerLap);
    }

    public static TilingCellGrid resolve(@Nullable TilingCellGrid cached,
            WorldFold fold, int vanillaCellWidthBlocks) {
        return cached != null && cached.fold == fold
                ? cached
                : of(fold, vanillaCellWidthBlocks);
    }

    @Override
    public int cellOriginX(int blockX, int blockZ) {
        if (this.skewBlocks == TranslationLattice.NO_SKEW) {
            return Math.floorDiv(blockX, this.xCellWidthBlocks) * this.xCellWidthBlocks;
        }

        int rowOffsetBlocks = (int) Math.floorDiv((long) Math.floorDiv(blockZ, this.zCellWidthBlocks) * this.skewBlocks, this.rowsPerLap);
        return Math.floorDiv(blockX - rowOffsetBlocks, this.xCellWidthBlocks) * this.xCellWidthBlocks + rowOffsetBlocks;
    }

    public int cellOriginZ(int blockZ) {
        return Math.floorDiv(blockZ, this.zCellWidthBlocks) * this.zCellWidthBlocks;
    }

    @Override
    public int cellOriginZ(int blockX, int blockZ) {
        return cellOriginZ(blockZ);
    }

    @Override
    public GenerationFoldContext.Context.DivisorScope divide(GenerationFoldContext.Context context) {
        return context.withDivisors(this.xCellWidthBlocks, this.zCellWidthBlocks);
    }

    static int tilingWidthBlocks(WrapDomain domain, int vanillaCellWidthBlocks) {
        return domain.isLooped()
                ? tilingWidthBlocks(domain.domainLength, vanillaCellWidthBlocks)
                : vanillaCellWidthBlocks;
    }

    public static int tilingWidthBlocks(int widthBlocks, int vanillaCellWidthBlocks) {
        double vanillaCellCount = (double) widthBlocks / vanillaCellWidthBlocks;
        int cellCount = SMALLEST_CELL_COUNT;

        for (int divisor : Divisors.of(widthBlocks)) {
            cellCount = nearer(cellCount, divisor, vanillaCellCount);
        }

        return widthBlocks / cellCount;
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
