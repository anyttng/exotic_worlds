package com.exoticworlds.compat.wover;

import com.exoticworlds.compat.worldgen.LapCells;
import com.exoticworlds.compat.worldgen.OpenSimplexStandIn;
import com.exoticworlds.core.fold.WrapDomain;

record LapAxis(WrapDomain blocks, double cellBlocks, int cells, int chunks, int chunkSide) {
    static LapAxis of(WrapDomain blocks, double cellBlocks, boolean areCellsEven, int chunkSide) {
        if (!blocks.isLooped()) {
            return new LapAxis(blocks, cellBlocks, 0, 0, chunkSide);
        }

        int cells = Math.toIntExact(LapCells.whole(blocks.domainLength / cellBlocks, 1));
        if (areCellsEven && (cells & 1) == 1) {
            cells++;
        }

        return new LapAxis(blocks, cellBlocks, cells, Math.ceilDiv(cells, chunkSide), chunkSide);
    }

    boolean isLooped() {
        return this.blocks.isLooped();
    }

    boolean isWrapping() {
        return this.chunks == 1;
    }

    double toLattice(double block) {
        return isLooped()
                ? (this.blocks.wrap(block) - this.blocks.lowerBound) * this.cells / this.blocks.domainLength
                : block / this.cellBlocks;
    }

    double period() {
        return isLooped() ? this.cells : OpenSimplexStandIn.UNBOUNDED;
    }

    double latticeBlocks() {
        return isLooped() ? (double) this.blocks.domainLength / this.cells : this.cellBlocks;
    }

    int wrapCell(int cell) {
        return isLooped() ? Math.floorMod(cell, this.cells) : cell;
    }

    int chunkOf(int cell) {
        if (!isLooped()) {
            return Math.floorDiv(cell, this.chunkSide);
        }

        int chunk = (int) ((long) cell * this.chunks / this.cells);
        while (startOf(chunk + 1) <= cell) {
            chunk++;
        }

        while (startOf(chunk) > cell) {
            chunk--;
        }

        return chunk;
    }

    int startOf(int chunk) {
        return isLooped() ? (int) ((long) this.cells * chunk / this.chunks) : chunk * this.chunkSide;
    }

    int sideOf(int chunk) {
        return startOf(chunk + 1) - startOf(chunk);
    }

    boolean isBorder(int chunk, int local) {
        return !isWrapping() && local == sideOf(chunk) - 1;
    }

    int next(int chunk) {
        return isLooped() ? (chunk + 1) % this.chunks : chunk + 1;
    }
}
