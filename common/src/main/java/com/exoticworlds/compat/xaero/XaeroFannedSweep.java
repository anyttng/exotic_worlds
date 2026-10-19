package com.exoticworlds.compat.xaero;

import java.util.function.LongConsumer;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.world.level.ChunkPos;

public final class XaeroFannedSweep {
    private final LongOpenHashSet fanned = new LongOpenHashSet();
    private final LongOpenHashSet seen = new LongOpenHashSet();

    public void begin() {
        this.fanned.clear();
        this.seen.clear();
    }

    public void seen(int regX, int regZ) {
        this.seen.add(ChunkPos.pack(regX, regZ));
    }

    public void fanned(long[] regions) {
        for (long region : regions) {
            this.fanned.add(region);
        }
    }

    public void forEachUnseen(LongConsumer keep) {
        LongIterator regions = this.fanned.iterator();
        while (regions.hasNext()) {
            long region = regions.nextLong();
            if (!this.seen.contains(region)) {
                keep.accept(region);
            }
        }
    }
}
