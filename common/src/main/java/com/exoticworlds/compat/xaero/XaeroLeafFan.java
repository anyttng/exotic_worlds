package com.exoticworlds.compat.xaero;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.world.level.ChunkPos;

public final class XaeroLeafFan {
    public static final long NO_LEAF = Long.MIN_VALUE;

    private static final long[] NO_LEAVES = new long[0];
    private static final long NO_OWNER = Long.MIN_VALUE;
    private static final int NO_COLUMN = Integer.MIN_VALUE;

    private final LongOpenHashSet taken = new LongOpenHashSet();
    private long owner = NO_OWNER;
    private long[] leaves = NO_LEAVES;
    private int column = NO_COLUMN;
    private int firstRow;

    @FunctionalInterface
    public interface FannedLeaves {
        long[] of(int leveledRegX, int leveledRegZ);
    }

    public void begin() {
        this.taken.clear();
        this.owner = NO_OWNER;
    }

    public int extendColumn(int leveledSideInRegions, int leveledRegX, int leveledRegZ, int leafRegionMinX,
            int leafRegionMinZ, int leafX, int minLeafRegX, int maxLeafRegX, FannedLeaves fanned) {
        long owner = ChunkPos.pack(leveledRegX, leveledRegZ);
        if (owner != this.owner) {
            this.owner = owner;
            this.leaves = NO_LEAVES;
            this.column = NO_COLUMN;
            this.firstRow = leafRegionMinZ + leveledSideInRegions;
        }

        int regX = leafRegionMinX + leafX;
        if (this.column == NO_COLUMN && regX >= minLeafRegX && regX <= maxLeafRegX) {
            this.column = regX;
            this.leaves = untaken(fanned.of(leveledRegX, leveledRegZ));
        }

        return regX == this.column ? leveledSideInRegions + this.leaves.length : leveledSideInRegions;
    }

    private long[] untaken(long[] fanned) {
        LongArrayList untaken = new LongArrayList(fanned.length);
        for (long leaf : fanned) {
            if (this.taken.add(leaf)) {
                untaken.add(leaf);
            }
        }

        return untaken.toLongArray();
    }

    public long leaf(int regX, int regZ) {
        int index = regZ - this.firstRow;
        return regX == this.column && index >= 0 && index < this.leaves.length ? this.leaves[index] : NO_LEAF;
    }

    public int admitRow(int maxLeafRegZ, int regX, int regZ) {
        return leaf(regX, regZ) == NO_LEAF ? maxLeafRegZ : Integer.MAX_VALUE;
    }

    public boolean exists(boolean exists, int regX, int regZ) {
        return exists && leaf(regX, regZ) == NO_LEAF;
    }
}
