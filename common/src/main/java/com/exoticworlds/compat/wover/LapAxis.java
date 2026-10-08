package com.exoticworlds.compat.wover;

import com.exoticworlds.core.WrapDomain;

record LapAxis(WrapDomain blocks, double cellBlocks, int cells, int chunks, int chunkSide) {
    private static final double CELL_EPSILON = 1.0E-9;

    static LapAxis of(WrapDomain blocks, double cellBlocks, boolean evenCells, int chunkSide) {
        if (!blocks.loops()) {
            return new LapAxis(blocks, cellBlocks, 0, 0, chunkSide);
        }

        int cells = Math.max(1, (int) Math.ceil(blocks.domainLength / cellBlocks - CELL_EPSILON));
        if (evenCells && (cells & 1) == 1) {
            cells++;
        }

        return new LapAxis(blocks, cellBlocks, cells, Math.ceilDiv(cells, chunkSide), chunkSide);
    }

    boolean loops() {
        return this.blocks.loops();
    }

    boolean wraps() {
        return this.chunks == 1;
    }

    double toLattice(double block) {
        return loops()
                ? (this.blocks.wrap(block) - this.blocks.lowerBound) * this.cells / this.blocks.domainLength
                : block / this.cellBlocks;
    }

    double period() {
        return loops() ? this.cells : OpenSimplexStandIn.UNBOUNDED;
    }

    double latticeBlocks() {
        return loops() ? (double) this.blocks.domainLength / this.cells : this.cellBlocks;
    }

    int wrapCell(int cell) {
        return loops() ? Math.floorMod(cell, this.cells) : cell;
    }

    int chunkOf(int cell) {
        if (!loops()) {
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
        return loops() ? (int) ((long) this.cells * chunk / this.chunks) : chunk * this.chunkSide;
    }

    int sideOf(int chunk) {
        return startOf(chunk + 1) - startOf(chunk);
    }

    boolean isBorder(int chunk, int local) {
        return !wraps() && local == sideOf(chunk) - 1;
    }

    int next(int chunk) {
        return loops() ? (chunk + 1) % this.chunks : chunk + 1;
    }
}
