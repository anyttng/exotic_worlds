package com.exoticworlds.engine.gen;

import java.util.BitSet;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.gen.TerrainSnapshot;
import com.exoticworlds.core.CoordinateConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

public final class TerrainMask implements TerrainSnapshot {
    private static final String MIN_Y_KEY = "min_y";

    private static final String CELLS_KEY = "cells";

    private static final String SOLID_KEY = "solid";

    private static final String WRITTEN_KEY = "written";

    private static final String LOWEST_Y_KEY = "lowest_y";

    private static final String HIGHEST_Y_KEY = "highest_y";

    private static final int NO_VALUE = Integer.MIN_VALUE;

    private final ChunkPos pos;

    private final int minY;

    private final CellGrid grid;

    private final BitSet solid;

    private final int lowestY;

    private final int highestY;

    private @Nullable BitSet written;

    private TerrainMask(ChunkPos pos, int minY, CellGrid grid, BitSet solid, int lowestY, int highestY) {
        this.pos = pos;
        this.minY = minY;
        this.grid = grid;
        this.solid = solid;
        this.lowestY = lowestY;
        this.highestY = highestY;
    }

    static TerrainMask of(boolean[] solid, ChunkPos pos, int minY, CellGrid grid) {
        BitSet bits = new BitSet(grid.cells());
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;

        for (int cell = 0; cell < solid.length; cell++) {
            if (!solid[cell]) {
                continue;
            }

            bits.set(cell);
            int y = minY + grid.layer(cell);
            lowestY = Math.min(lowestY, y);
            highestY = Math.max(highestY, y);
        }

        return new TerrainMask(pos, minY, grid, bits, lowestY, highestY);
    }

    static @Nullable TerrainMask load(CompoundTag tag, ChunkPos pos, int minY, int height) {
        CellGrid grid = new CellGrid(CoordinateConstants.CHUNK_WIDTH, height);
        long[] solid = tag.getLongArray(SOLID_KEY).orElse(null);
        if (solid == null || tag.getIntOr(MIN_Y_KEY, NO_VALUE) != minY
                || tag.getIntOr(CELLS_KEY, NO_VALUE) != grid.cells()) {
            return null;
        }

        TerrainMask mask = new TerrainMask(pos, minY, grid, BitSet.valueOf(solid),
                tag.getIntOr(LOWEST_Y_KEY, Integer.MAX_VALUE), tag.getIntOr(HIGHEST_Y_KEY, Integer.MIN_VALUE));
        tag.getLongArray(WRITTEN_KEY).ifPresent(written -> mask.written = BitSet.valueOf(written));
        return mask;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(MIN_Y_KEY, this.minY);
        tag.putInt(CELLS_KEY, this.grid.cells());
        tag.putLongArray(SOLID_KEY, this.solid.toLongArray());
        tag.putInt(LOWEST_Y_KEY, this.lowestY);
        tag.putInt(HIGHEST_Y_KEY, this.highestY);
        BitSet overwritten = this.written;
        if (overwritten != null) {
            tag.putLongArray(WRITTEN_KEY, overwritten.toLongArray());
        }

        return tag;
    }

    public void wrote(BlockPos pos) {
        int cell = this.cellOf(pos.getX() & (CoordinateConstants.CHUNK_WIDTH - 1), pos.getY(),
                pos.getZ() & (CoordinateConstants.CHUNK_WIDTH - 1));
        if (cell == CellGrid.NO_CELL) {
            return;
        }

        if (this.written == null) {
            this.written = new BitSet(this.grid.cells());
        }

        this.written.set(cell);
    }

    ChunkPos pos() {
        return this.pos;
    }

    boolean isEmpty() {
        return this.lowestY > this.highestY;
    }

    int lowestY() {
        return this.lowestY;
    }

    int highestY() {
        return this.highestY;
    }

    boolean solidAt(int localX, int y, int localZ) {
        int cell = this.cellOf(localX, y, localZ);
        return cell != CellGrid.NO_CELL && this.solid.get(cell);
    }

    boolean untouchedAt(int localX, int y, int localZ) {
        int cell = this.cellOf(localX, y, localZ);
        if (cell == CellGrid.NO_CELL || !this.solid.get(cell)) {
            return false;
        }

        BitSet overwritten = this.written;
        return overwritten == null || !overwritten.get(cell);
    }

    int cellOf(int localX, int y, int localZ) {
        int layer = y - this.minY;
        if (layer < 0 || layer >= this.grid.layers()) {
            return CellGrid.NO_CELL;
        }

        return this.grid.cell(localX, layer, localZ);
    }
}
