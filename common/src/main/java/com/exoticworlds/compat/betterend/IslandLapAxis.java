package com.exoticworlds.compat.betterend;

import com.exoticworlds.core.WrapDomain;

import net.minecraft.util.Mth;

public record IslandLapAxis(int lapBlocks, int cells, double cellBlocks) {
    private static final int UNBOUNDED = 0;

    private static final double CELL_EPSILON = 1.0E-9;

    public static IslandLapAxis of(WrapDomain blocks, double distance) {
        if (!blocks.loops()) {
            return new IslandLapAxis(UNBOUNDED, UNBOUNDED, distance);
        }

        int cells = Math.max(1, (int) Math.ceil(blocks.domainLength / distance - CELL_EPSILON));
        return new IslandLapAxis(blocks.domainLength, cells, (double) blocks.domainLength / cells);
    }

    public boolean loops() {
        return this.cells != UNBOUNDED;
    }

    public int cell(double block) {
        return Mth.floor(block / this.cellBlocks);
    }

    public int wrap(int cell) {
        return loops() ? Math.floorMod(cell, this.cells) : cell;
    }

    public int shift(int cell) {
        return loops() ? Math.floorDiv(cell, this.cells) * this.lapBlocks : 0;
    }

    public int nearestToOrigin(int canonicalCell) {
        return loops() && 2 * canonicalCell + 1 > this.cells ? canonicalCell - this.cells : canonicalCell;
    }

    public int originCopy(double block) {
        return loops() ? (int) Math.round(block / this.lapBlocks) * this.lapBlocks : 0;
    }
}
