package com.exoticworlds.compat.betterend;

import com.exoticworlds.compat.worldgen.LapCells;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.util.Mth;

public record IslandLapAxis(int lapBlocks, int cells, double cellBlocks) {
    private static final int UNBOUNDED = 0;

    public static IslandLapAxis of(WrapDomain blocks, double distance) {
        if (!blocks.loops()) {
            return new IslandLapAxis(UNBOUNDED, UNBOUNDED, distance);
        }

        int cells = Math.toIntExact(LapCells.whole(blocks.domainLength / distance, 1));
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

    public int laps(int cell) {
        return loops() ? Math.floorDiv(cell, this.cells) : 0;
    }

    public int nearestToOrigin(int canonicalCell) {
        return loops() && 2 * canonicalCell + 1 > this.cells ? canonicalCell - this.cells : canonicalCell;
    }

    public int originLapsOfCell(int cell) {
        return loops() ? (cell - nearestToOrigin(wrap(cell))) / this.cells : 0;
    }

    public int originLaps(double block) {
        return loops() ? (int) Math.round(block / this.lapBlocks) : 0;
    }
}
